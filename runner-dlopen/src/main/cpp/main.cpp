#include <iostream>
#include <dlfcn.h>
#include <string>

bool isIssue1515Error(char * err) {
    std::string str(err);
    return str.find("AMotionEvent_fromJava") != std::string::npos
        || str.find("AInputEvent_release") != std::string::npos;
}

/**
 * The point of this wrapper is being able to actually execute K/N executables on Android Native.
 * They are currently broken, see https://youtrack.jetbrains.com/issue/KT-49144 .
 *
 * Normally it shouldn't be possible to dlopen/dlsym an executable main function, but it works
 * here because these are PIE excutables and we know the function name (Konan_main) that does the
 * appropriate runtime setup and teardown.
 *
 * Note that dlopen() can fail in same cases because of a bug introduced in latest NDK releases
 * (present as of 23.0.7599858) in which two functions, AMotionEvent_fromJava and AInputEvent_release,
 * are not guarded by API level in the header file, despite being introduced in API 31.
 * See: https://github.com/android/ndk/issues/1515
 *
 * Normally this is a linker issue and it would only happen if you actually use these symbols,
 * but since we load the library dynamically, it surfaces here as well. We try to catch this and
 * ignore it.
 *
 * (Probably contributing to this issue, one of these functions is also incorrectly exported
 * by libandroid.so in some system images BEFORE API 31, like system/lib64/arm64/libandroid.so
 * in system-images;android-30;google_apis;x86_64. https://issuetracker.google.com/issues/198022909#comment13).
 *
 * Interesting links:
 * - https://developer.android.com/ndk/reference/group/libdl
 * - https://source.android.com/devices/architecture/vndk/linker-namespace#how-does-it-work
 */
int main(int argc, const char * argv[]) {
    std::cout << "Runner: started.\n";
    auto testExecutable = (argc >= 2) ? argv[1] : "test.so";
    auto testFunction = (argc >= 3) ? argv[2] : "Konan_main";

    void* lib = dlopen(testExecutable, RTLD_NOW);
    auto err = dlerror();
    if (!lib || (err && !isIssue1515Error(err))) {
        if (err) std::cout << "Runner: error while loading '" << testExecutable << "': " << err << "\n";
        else std::cout << "Runner: unknown error while loading '" << testExecutable << "'.\n";
        return 1;
    }

    typedef void (*VoidFunction)();
    auto test = (VoidFunction) dlsym(lib, testFunction);
    err = dlerror();
    if (err) {
        std::cout << "Runner: function '" << testFunction << "' not found: " << err << "\n";
        dlclose(lib);
        return 1;
    }

    std::cout << "Runner: about to invoke test executable...\n";
    test();
    dlclose(lib);
    return 0;
}