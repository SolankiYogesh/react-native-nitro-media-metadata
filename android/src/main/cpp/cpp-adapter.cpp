#include <jni.h>
#include "nitromediametadataOnLoad.hpp"

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
  return margelo::nitro::nitromediametadata::initialize(vm);
}
