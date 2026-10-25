#!/bin/bash
# Compile and run the reference code + tests with the Kotlin compiler bundled in Gradle.
set -e
V=$(dirname "$(readlink -f "$0")")
L=/opt/gradle/lib
KCP=$L/kotlin-compiler-embeddable-2.0.21.jar:$L/kotlin-stdlib-2.0.21.jar:$L/kotlin-script-runtime-2.0.21.jar:$L/kotlin-reflect-2.0.21.jar:$L/kotlin-daemon-embeddable-2.0.21.jar:$L/trove4j-1.0.20200330.jar:$L/annotations-24.0.1.jar:$L/kotlinx-coroutines-core-jvm-1.6.4.jar
LIBS=$L/kotlin-stdlib-2.0.21.jar:$L/kotlinx-coroutines-core-jvm-1.6.4.jar:$L/junit-4.13.2.jar:$L/hamcrest-core-1.3.jar:$L/gson-2.10.jar
rm -rf $V/out && mkdir -p $V/out
java -cp "$KCP" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect -jvm-target 17 -nowarn \
  -cp "$LIBS" -d $V/out $(find $V/main $V/stubs $V/test -name '*.kt') 2>&1 | grep -v "^warning" || true
ls $V/out | head -1 >/dev/null
CLASSES=$(cd $V/out && find . -name '*Test.class' | sed 's#^\./##; s#\.class$##; s#/#.#g' | grep -v '\$' | sort | tr '\n' ' ')
java -cp "$V/out:$V/res:$LIBS" org.junit.runner.JUnitCore $CLASSES 2>&1 | grep -v "^\s*at \(org.junit\|java.base\|jdk.internal\|kotlin\.\)" | head -${LINES_MAX:-80}
