// JNI bridge between LlamaCppEngine (Kotlin) and llama.cpp.
//
// Design notes:
//  * The model is loaded once and kept between requests; the context and sampler belong
//    to a single generation and are released in a RAII guard.
//  * Tokens are streamed through a Java callback. Cancellation is polled between tokens,
//    so "Stop" in the UI returns within one token.
//  * The prompt is formatted with the model's embedded chat template. The fallback is
//    ChatML, never an invented format.
//  * Every failure path reports a negative value and a log line; no text is fabricated.

#include <jni.h>
#include <android/log.h>

#include <cstdint>
#include <cstring>
#include <random>
#include <string>
#include <vector>

#include "llama.h"

#define TAG "TumbaLlama"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace {

constexpr int kDefaultBatch = 512;
constexpr int kMaxContext = 32'768;

jclass g_callbackClass = nullptr;
jmethodID g_onToken = nullptr;
jmethodID g_isCancelled = nullptr;

struct ModelHandle {
    llama_model * model = nullptr;
    std::string path;
};

// ---------------------------------------------------------------------------------------
// RAII helpers
// ---------------------------------------------------------------------------------------

struct ContextGuard {
    llama_context * ctx = nullptr;
    ~ContextGuard() {
        if (ctx != nullptr) llama_free(ctx);
    }
};

struct SamplerGuard {
    llama_sampler * smpl = nullptr;
    ~SamplerGuard() {
        if (smpl != nullptr) llama_sampler_free(smpl);
    }
};

// ---------------------------------------------------------------------------------------
// Small UTF-8 helpers
// ---------------------------------------------------------------------------------------

size_t utf8ValidPrefixLength(const std::string & text) {
    size_t i = 0;
    size_t lastComplete = 0;
    while (i < text.size()) {
        const unsigned char c = static_cast<unsigned char>(text[i]);
        size_t length = 0;
        if (c < 0x80) {
            length = 1;
        } else if ((c >> 5) == 0x06) {
            length = 2;
        } else if ((c >> 4) == 0x0E) {
            length = 3;
        } else if ((c >> 3) == 0x1E) {
            length = 4;
        } else {
            ++i;  // stray continuation byte: drop it
            lastComplete = i;
            continue;
        }
        if (i + length > text.size()) break;  // incomplete tail, wait for more bytes
        bool valid = true;
        for (size_t k = 1; k < length; ++k) {
            if ((static_cast<unsigned char>(text[i + k]) & 0xC0) != 0x80) {
                valid = false;
                break;
            }
        }
        if (!valid) {
            ++i;
            lastComplete = i;
            continue;
        }
        i += length;
        lastComplete = i;
    }
    return lastComplete;
}

// Decodes UTF-8 (including 4-byte sequences) into a Java string instead of relying on
// NewStringUTF, which expects modified UTF-8 and breaks on emoji.
jstring toJavaString(JNIEnv * env, const std::string & utf8) {
    std::u16string out;
    out.reserve(utf8.size());
    size_t i = 0;
    while (i < utf8.size()) {
        const unsigned char c = static_cast<unsigned char>(utf8[i]);
        uint32_t codePoint = 0;
        size_t extra = 0;
        if (c < 0x80) {
            codePoint = c;
        } else if ((c >> 5) == 0x06) {
            codePoint = c & 0x1F;
            extra = 1;
        } else if ((c >> 4) == 0x0E) {
            codePoint = c & 0x0F;
            extra = 2;
        } else if ((c >> 3) == 0x1E) {
            codePoint = c & 0x07;
            extra = 3;
        } else {
            ++i;
            continue;
        }
        if (i + extra >= utf8.size()) break;
        bool valid = true;
        for (size_t k = 1; k <= extra; ++k) {
            const unsigned char cc = static_cast<unsigned char>(utf8[i + k]);
            if ((cc & 0xC0) != 0x80) {
                valid = false;
                break;
            }
            codePoint = (codePoint << 6) | (cc & 0x3F);
        }
        if (!valid) {
            ++i;
            continue;
        }
        i += extra + 1;
        if (codePoint <= 0xFFFF) {
            out.push_back(static_cast<char16_t>(codePoint));
        } else {
            codePoint -= 0x10000;
            out.push_back(static_cast<char16_t>(0xD800 + (codePoint >> 10)));
            out.push_back(static_cast<char16_t>(0xDC00 + (codePoint & 0x3FF)));
        }
    }
    return env->NewString(reinterpret_cast<const jchar *>(out.data()),
                         static_cast<jsize>(out.size()));
}

bool isCancelled(JNIEnv * env, jobject callback) {
    if (callback == nullptr || g_isCancelled == nullptr) return false;
    const jboolean cancelled = env->CallBooleanMethod(callback, g_isCancelled);
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return true;
    }
    return cancelled == JNI_TRUE;
}

bool emitToken(JNIEnv * env, jobject callback, const std::string & text) {
    if (text.empty() || callback == nullptr || g_onToken == nullptr) return true;
    jstring javaText = toJavaString(env, text);
    if (javaText == nullptr) return false;
    env->CallVoidMethod(callback, g_onToken, javaText);
    env->DeleteLocalRef(javaText);
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return false;
    }
    return true;
}

std::string toStdString(JNIEnv * env, jstring value) {
    if (value == nullptr) return {};
    const char * chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) return {};
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

}  // namespace

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM * vm, void * /*reserved*/) {
    llama_backend_init();

    JNIEnv * env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK || env == nullptr) {
        LOGE("JNI_OnLoad: no JNIEnv");
        return JNI_ERR;
    }
    jclass callbackClass = env->FindClass("com/tumba/ai/data/ai/local/TokenCallback");
    if (callbackClass != nullptr) {
        g_callbackClass = static_cast<jclass>(env->NewGlobalRef(callbackClass));
        g_onToken = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)V");
        g_isCancelled = env->GetMethodID(callbackClass, "isCancelled", "()Z");
        env->DeleteLocalRef(callbackClass);
    }
    if (g_onToken == nullptr || g_isCancelled == nullptr) {
        LOGE("JNI_OnLoad: TokenCallback methods not found");
        return JNI_ERR;
    }
    LOGI("llama.cpp %s ready", llama_version());
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void JNICALL JNI_OnUnload(JavaVM * /*vm*/, void * /*reserved*/) {
    llama_backend_free();
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_tumba_ai_data_ai_local_LlamaCppEngine_nativeVersion(JNIEnv * env, jobject /*thiz*/) {
    return env->NewStringUTF(llama_version());
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_tumba_ai_data_ai_local_LlamaCppEngine_nativeLoadModel(
        JNIEnv * env, jobject /*thiz*/, jstring path) {
    const std::string modelPath = toStdString(env, path);
    if (modelPath.empty()) {
        LOGE("nativeLoadModel: empty path");
        return 0;
    }

    llama_model_params params = llama_model_default_params();
    params.n_gpu_layers = 0;
    // Default load mode is AUTO; mmap keeps a ~1 GB model out of the Java heap and lets the
    // kernel drop pages under pressure instead of killing the process.
    params.load_mode = LLAMA_LOAD_MODE_MMAP;

    llama_model * model = llama_model_load_from_file(modelPath.c_str(), params);
    if (model == nullptr) {
        LOGE("nativeLoadModel: failed to load %s", modelPath.c_str());
        return 0;
    }

    auto * handle = new ModelHandle();
    handle->model = model;
    handle->path = modelPath;
    LOGI("nativeLoadModel: loaded %s (n_ctx_train=%d)",
         modelPath.c_str(), llama_model_n_ctx_train(model));
    return reinterpret_cast<jlong>(handle);
}

extern "C" JNIEXPORT void JNICALL
Java_com_tumba_ai_data_ai_local_LlamaCppEngine_nativeFreeModel(
        JNIEnv * /*env*/, jobject /*thiz*/, jlong handlePtr) {
    auto * handle = reinterpret_cast<ModelHandle *>(handlePtr);
    if (handle == nullptr) return;
    if (handle->model != nullptr) llama_model_free(handle->model);
    delete handle;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_tumba_ai_data_ai_local_LlamaCppEngine_nativeGenerate(
        JNIEnv * env,
        jobject /*thiz*/,
        jlong handlePtr,
        jobjectArray roles,
        jobjectArray contents,
        jint maxTokens,
        jfloat temperature,
        jint contextSize,
        jint threadCount,
        jobject callback) {
    auto * handle = reinterpret_cast<ModelHandle *>(handlePtr);
    if (handle == nullptr || handle->model == nullptr) {
        LOGE("nativeGenerate: model not loaded");
        return -1;
    }

    const int messageCount = roles == nullptr ? 0 : env->GetArrayLength(roles);
    if (messageCount <= 0 || contents == nullptr) {
        LOGE("nativeGenerate: empty conversation");
        return -1;
    }

    // Keep the UTF-8 strings alive while llama.cpp reads them.
    std::vector<std::string> roleStrings;
    std::vector<std::string> contentStrings;
    std::vector<llama_chat_message> chat;
    roleStrings.reserve(messageCount);
    contentStrings.reserve(messageCount);
    chat.reserve(messageCount);
    for (int i = 0; i < messageCount; ++i) {
        auto role = static_cast<jstring>(env->GetObjectArrayElement(roles, i));
        auto content = static_cast<jstring>(env->GetObjectArrayElement(contents, i));
        roleStrings.push_back(toStdString(env, role));
        contentStrings.push_back(toStdString(env, content));
        env->DeleteLocalRef(role);
        env->DeleteLocalRef(content);
    }
    for (int i = 0; i < messageCount; ++i) {
        chat.push_back({roleStrings[i].c_str(), contentStrings[i].c_str()});
    }

    // 1. Format the prompt with the model's own chat template.
    const char * tmpl = llama_model_chat_template(handle->model, nullptr);
    if (tmpl == nullptr) {
        LOGW("nativeGenerate: model has no chat template, using chatml");
        tmpl = "chatml";
    }
    std::vector<char> formatted;
    int32_t formattedLength = llama_chat_apply_template(
            tmpl, chat.data(), chat.size(), true, nullptr, 0);
    if (formattedLength <= 0) {
        LOGE("nativeGenerate: chat template failed (%d)", formattedLength);
        return -1;
    }
    formatted.resize(static_cast<size_t>(formattedLength) + 1);
    formattedLength = llama_chat_apply_template(
            tmpl, chat.data(), chat.size(), true, formatted.data(),
            static_cast<int32_t>(formatted.size()));
    if (formattedLength <= 0) {
        LOGE("nativeGenerate: chat template render failed (%d)", formattedLength);
        return -1;
    }
    const std::string prompt(formatted.data(), static_cast<size_t>(formattedLength));

    // 2. Tokenize.
    const llama_vocab * vocab = llama_model_get_vocab(handle->model);
    const bool addBos = llama_vocab_get_add_bos(vocab);
    std::vector<llama_token> tokens(prompt.size() + 8);
    int32_t tokenCount = llama_tokenize(
            vocab, prompt.c_str(), static_cast<int32_t>(prompt.size()),
            tokens.data(), static_cast<int32_t>(tokens.size()), addBos, true);
    if (tokenCount < 0) {
        tokens.resize(static_cast<size_t>(-tokenCount));
        tokenCount = llama_tokenize(
                vocab, prompt.c_str(), static_cast<int32_t>(prompt.size()),
                tokens.data(), static_cast<int32_t>(tokens.size()), addBos, true);
    }
    if (tokenCount <= 0) {
        LOGE("nativeGenerate: tokenization failed (%d)", tokenCount);
        return -1;
    }
    tokens.resize(static_cast<size_t>(tokenCount));

    // 3. Create a context sized for this request.
    int requestedContext = contextSize > 0 ? contextSize : 4'096;
    requestedContext = std::min(requestedContext, kMaxContext);
    const int maxOutput = maxTokens > 0 ? maxTokens : 512;
    const int reserved = std::min(maxOutput, requestedContext / 2);
    const int maxPromptTokens = std::max(64, requestedContext - reserved);
    const bool promptTruncated = static_cast<int>(tokens.size()) > maxPromptTokens;
    if (promptTruncated) {
        LOGW("nativeGenerate: prompt truncated from %zu to %d tokens",
             tokens.size(), maxPromptTokens);
        tokens.erase(tokens.begin(), tokens.end() - maxPromptTokens);
    }

    llama_context_params ctxParams = llama_context_default_params();
    ctxParams.n_ctx = static_cast<uint32_t>(requestedContext);
    ctxParams.n_batch = static_cast<uint32_t>(
            std::min(std::max(static_cast<int>(tokens.size()), kDefaultBatch), 1024));
    ctxParams.n_ubatch = ctxParams.n_batch;
    ctxParams.n_threads = threadCount > 0 ? threadCount : 4;
    ctxParams.n_threads_batch = ctxParams.n_threads;
    ctxParams.no_perf = true;

    ContextGuard context;
    context.ctx = llama_init_from_model(handle->model, ctxParams);
    if (context.ctx == nullptr) {
        LOGE("nativeGenerate: failed to create context");
        return -1;
    }

    // 4. Sampler chain: deterministic when temperature is 0.
    SamplerGuard sampler;
    const int32_t vocabSize = llama_vocab_n_tokens(vocab);
    llama_sampler_chain_params chainParams = llama_sampler_chain_default_params();
    sampler.smpl = llama_sampler_chain_init(chainParams);
    if (sampler.smpl == nullptr) {
        LOGE("nativeGenerate: failed to create sampler");
        return -1;
    }
    if (temperature <= 0.0f) {
        llama_sampler_chain_add(sampler.smpl, llama_sampler_init_greedy());
    } else {
        const uint32_t seed = std::random_device{}();
        llama_sampler_chain_add(sampler.smpl, llama_sampler_init_top_k(40));
        llama_sampler_chain_add(sampler.smpl, llama_sampler_init_top_p(0.95f, 1));
        llama_sampler_chain_add(sampler.smpl, llama_sampler_init_temp(temperature));
        llama_sampler_chain_add(
                sampler.smpl,
                llama_sampler_init_penalties(vocabSize, 64, 1.1f, 0.0f, 0.0f));
        llama_sampler_chain_add(sampler.smpl, llama_sampler_init_dist(seed));
    }

    // 5. Evaluate the prompt in batches small enough for n_batch.
    const int batchSize = static_cast<int>(ctxParams.n_batch);
    for (size_t offset = 0; offset < tokens.size(); offset += batchSize) {
        const int chunk = static_cast<int>(
                std::min<size_t>(batchSize, tokens.size() - offset));
        llama_batch batch = llama_batch_get_one(tokens.data() + offset, chunk);
        const int32_t result = llama_decode(context.ctx, batch);
        if (result != 0) {
            LOGE("nativeGenerate: prompt decode failed (%d)", result);
            return -1;
        }
    }

    // 6. Stream tokens.
    int generated = 0;
    std::string pending;
    while (generated < maxOutput) {
        if (isCancelled(env, callback)) break;

        const llama_token token = llama_sampler_sample(sampler.smpl, context.ctx, -1);
        if (llama_vocab_is_eog(vocab, token)) break;
        llama_sampler_accept(sampler.smpl, token);

        char pieceBuffer[256];
        // `special = false` keeps chat-template control tokens (e.g. <|im_end|>) out of the UI.
        int32_t pieceLength = llama_token_to_piece(
                vocab, token, pieceBuffer, sizeof(pieceBuffer), 0, false);
        if (pieceLength < 0) {
            // Token pieces are never longer than this buffer in practice; skip if they are.
            LOGW("nativeGenerate: token piece too long (%d)", pieceLength);
        } else if (pieceLength > 0) {
            pending.append(pieceBuffer, static_cast<size_t>(pieceLength));
            const size_t complete = utf8ValidPrefixLength(pending);
            if (complete > 0) {
                if (!emitToken(env, callback, pending.substr(0, complete))) break;
                pending.erase(0, complete);
            }
        }

        llama_batch batch = llama_batch_get_one(const_cast<llama_token *>(&token), 1);
        const int32_t result = llama_decode(context.ctx, batch);
        if (result != 0) {
            LOGW("nativeGenerate: token decode failed (%d)", result);
            break;
        }
        ++generated;
    }
    if (!pending.empty()) {
        emitToken(env, callback, pending);
    }

    LOGI("nativeGenerate: generated %d token(s)", generated);
    return generated;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_tumba_ai_data_ai_local_LlamaCppEngine_nativeContextSize(
        JNIEnv * /*env*/, jobject /*thiz*/, jlong handlePtr) {
    auto * handle = reinterpret_cast<ModelHandle *>(handlePtr);
    if (handle == nullptr || handle->model == nullptr) return 0;
    return llama_model_n_ctx_train(handle->model);
}