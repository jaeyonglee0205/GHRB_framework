CDIR=$(pwd)

for PID in $(./cli.py pid -q); do
for BID in $(cut -f1 -d',' "/root/framework/commit_db/${PID}_bugs"); do

BUGDIR="${PID}_${BID}"
echo ${BUGDIR}

mkdir "${CDIR}/${PID}_${BID}"

cd "${CDIR}/${PID}_${BID}"

# if [ -f "./sfl/txt/matrix.txt" ]; then
#         echo ${BUGDIR} already done!
#         continue
# fi

# git clean -df
# defects4j compile
# LOGFILE="gzlog.txt"
# echo "" > ${LOGFILE}

# mvn dependency:build-classpath -DincludeScope=test -Dmdep.outputFile=/root/framework/testing/cp.txt
# mvn help:evaluate -Dexpression=project.build.testOutputDirectory -q -DforceStdout -DoutputDirectory=.         /root/framework/testing/target/test-classes
# mvn help:evaluate -Dexpression=project.build.directory -q -DforceStdout                                       /root/framework/testing/target
# mvn help:evaluate -Dexpression=project.build.outputDirectory -q -DforceStdout                                 /root/framework/testing/target/classes

# cp.compile: /root/defects4j/framework/bin/gson_1_buggy/target/classes
# Running ant (export.cp.compile)

# cp.test:  /root/defects4j/framework/projects/Gson/lib/junit/junit/3.8.2/junit-3.8.2.jar:
#           /root/defects4j/framework/bin/gson_1_buggy/target/classes:/root/defects4j/framework/bin/gson_1_buggy/target/test-classes:
#           /root/defects4j/framework/projects/lib/junit-4.11.jar:/root/defects4j/framework/projects/Gson/lib/com/google/code/findbugs/jsr305/3.0.0/jsr305-3.0.0.jar:
#           /root/defects4j/framework/projects/Gson/lib/junit/junit/4.12/junit-4.12.jar:/root/defects4j/framework/projects/Gson/lib/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar

# cp.test: /root/defects4j/framework/bin/time27/build/classes:
#          /root/defects4j/framework/bin/time27/build/tests:
#          /root/defects4j/framework/projects/lib/junit-4.11.jar:
#          /root/defects4j/framework/bin/time27/src/test/java


# /root/defects4j/framework/projects/Jsoup/lib/junit/junit/4.12/junit-4.12.jar:
# /root/defects4j/framework/projects/Jsoup/lib/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar:
# /root/defects4j/framework/projects/Jsoup/lib/com/google/code/gson/gson/2.7/gson-2.7.jar:
# /root/defects4j/framework/projects/Jsoup/lib/org/eclipse/jetty/jetty-server/9.2.28.v20190418/jetty-server-9.2.28.v20190418.jar:
# /root/defects4j/framework/projects/Jsoup/lib/javax/servlet/javax.servlet-api/3.1.0/javax.servlet-api-3.1.0.jar:
# /root/defects4j/framework/projects/Jsoup/lib/org/eclipse/jetty/jetty-http/9.2.28.v20190418/jetty-http-9.2.28.v20190418.jar:
# /root/defects4j/framework/projects/Jsoup/lib/org/eclipse/jetty/jetty-util/9.2.28.v20190418/jetty-util-9.2.28.v20190418.jar:
# /root/defects4j/framework/projects/Jsoup/lib/org/eclipse/jetty/jetty-io/9.2.28.v20190418/jetty-io-9.2.28.v20190418.jar:
# /root/defects4j/framework/projects/Jsoup/lib/org/eclipse/jetty/jetty-servlet/9.2.28.v20190418/jetty-servlet-9.2.28.v20190418.jar:
# /root/defects4j/framework/projects/Jsoup/lib/org/eclipse/jetty/jetty-security/9.2.28.v20190418/jetty-security-9.2.28.v20190418.jar:
# /root/defects4j/framework/bin/jsoup93/target/classes:
# /root/defects4j/framework/bin/jsoup93/target/test-classes:
# /root/defects4j/framework/projects/lib/junit-4.11.jar:
# /root/defects4j/framework/projects/Jsoup/lib/commons-lang/commons-lang/2.4/commons-lang-2.4.jar:
# /root/defects4j/framework/projects/Jsoup/lib/junit/junit/4.5/junit-4.5.jar:
# /root/defects4j/framework/projects/Jsoup/lib/org/eclipse/jetty/jetty-http/9.2.22.v20170606/jetty-http-9.2.22.v20170606.jar:
# /root/defects4j/framework/projects/Jsoup/lib/org/eclipse/jetty/jetty-http/9.2.26.v20180806/jetty-http-9.2.26.v20180806.jar:
# /root/defects4j/framework/projects/Jsoup/lib/org/eclipse/jetty/jetty-io/9.2.22.v20170606/jetty-io-9.2.22.v20170606.jar:
# /root/defects4j/framework/projects/Jsoup/lib/org/eclipse/jetty/jetty-io/9.2.26.v20180806/jetty-io-9.2.26.v20180806.jar:
# /root/defects4j/framework/projects/Jsoup/lib/org/eclipse/jetty/jetty-security/9.2.22.v20170606/jetty-security-9.2.22.v20170606.jar:
# /root/defects4j/framework/projects/Jsoup/lib/org/eclipse/jetty/jetty-security/9.2.26.v20180806/jetty-security-9.2.26.v20180806.jar:
# /root/defects4j/framework/projects/Jsoup/lib/org/eclipse/jetty/jetty-server/9.2.22.v20170606/jetty-server-9.2.22.v20170606.jar:
# /root/defects4j/framework/projects/Jsoup/lib/org/eclipse/jetty/jetty-server/9.2.26.v20180806/jetty-server-9.2.26.v20180806.jar:
# /root/defects4j/framework/projects/Jsoup/lib/org/eclipse/jetty/jetty-servlet/9.2.22.v20170606/jetty-servlet-9.2.22.v20170606.jar:
# /root/defects4j/framework/projects/Jsoup/lib/org/eclipse/jetty/jetty-servlet/9.2.26.v20180806/jetty-servlet-9.2.26.v20180806.jar:
# /root/defects4j/framework/projects/Jsoup/lib/org/eclipse/jetty/jetty-util/9.2.22.v20170606/jetty-util-9.2.22.v20170606.jar:
# /root/defects4j/framework/projects/Jsoup/lib/org/eclipse/jetty/jetty-util/9.2.26.v20180806/jetty-util-9.2.26.v20180806.jar
# dir.bin.classes: target/classes


# dir.bin.tests:   target/test-classes

# Collect metadata
test_classpath=$(mvn dependency:build-classpath -DincludeScope=test "-Dmdep.outputFile=${pwd}/cp.txt" | cat "cp.txt")
src_classes_dir=$(mvn help:evaluate -Dexpression=project.build.outputDirectory -q -DforceStdout)
src_classes_dir="$CDIR/$BUGDIR/$src_classes_dir"
test_classes_dir=$(mvn help:evaluate -Dexpression=project.build.testOutputDirectory -q -DforceStdout)
test_classes_dir="$CDIR/$BUGDIR/$test_classes_dir"

echo "$PID-${BID}b's classpath: $test_classpath" >&2
echo "$PID-${BID}b's bin dir: $src_classes_dir" >&2
echo "$PID-${BID}b's test bin dir: $test_classes_dir" >&2

# collect unit tests
unit_tests_file="$CDIR/$BUGDIR/unit_tests.txt"
relevant_tests="*"
java -cp "$test_classpath:$test_classes_dir:$D4J_HOME/framework/projects/lib/junit-4.11.jar:$GZOLTAR_CLI_JAR" \
  com.gzoltar.cli.Main listTestMethods \
    "$test_classes_dir" \
    --outputFile "$unit_tests_file" \
    --includes "$relevant_tests" >> ${LOGFILE}
#head "$unit_tests_file"

# classes to do fl on?
loaded_classes_file="$D4J_HOME/framework/projects/$PID/loaded_classes/$BID.src"
normal_classes=$(cat "$loaded_classes_file" | sed 's/$/:/' | sed ':a;N;$!ba;s/\n//g')
inner_classes=$(cat "$loaded_classes_file" | sed 's/$/$*:/' | sed ':a;N;$!ba;s/\n//g')
classes_to_debug="$normal_classes$inner_classes"
#echo "Likely faulty classes: $classes_to_debug" >&2

# run gzoltar
ser_file="$CDIR/$BUGDIR/gzoltar.ser"
java -XX:MaxPermSize=4096M -javaagent:$GZOLTAR_AGENT_JAR=destfile=$ser_file,buildlocation=$src_classes_dir,includes=$classes_to_debug,excludes="",inclnolocationclasses=false,output="FILE" \
  -cp "$src_classes_dir:$D4J_HOME/framework/projects/lib/junit-4.11.jar:$test_classpath:$GZOLTAR_CLI_JAR" \
  com.gzoltar.cli.Main runTestMethods \
    --testMethods "$unit_tests_file" \
    --collectCoverage # >> ${LOGFILE}

# report
java -cp "$src_classes_dir:$D4J_HOME/framework/projects/lib/junit-4.11.jar:$test_classpath:$GZOLTAR_CLI_JAR" \
    com.gzoltar.cli.Main faultLocalizationReport \
      --buildLocation "$src_classes_dir" \
      --granularity "line" \
      --inclPublicMethods \
      --inclStaticConstructors \
      --inclDeprecatedMethods \
      --dataFile "$ser_file" \
      --outputDirectory "$CDIR/$BUGDIR" \
      --family "sfl" \
      --formula "ochiai" \
      --metric "entropy" \
      --formatter "txt" # >> ${LOGFILE}

mv ${LOGFILE} "${CDIR}/${BUGDIR}/sfl/txt"

cd ${CDIR}

done
done