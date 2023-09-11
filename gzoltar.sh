CDIR=$(pwd)

for PID in $(defects4j pids); do
for BID in $(cut -f1 -d',' "$D4J_HOME/framework/projects/$PID/commit-db"); do

BUGDIR="${PID}_${BID}"
echo ${BUGDIR}

cd "${CDIR}/${PID}_${BID}"

if [ -f "./sfl/txt/matrix.txt" ]; then
        echo ${BUGDIR} already done!
        continue
fi

if [[ $PID != "Closure" ]]; then
        echo "Closure has been skipped for now."
        continue
fi

git clean -df
defects4j compile
LOGFILE="gzlog.txt"
echo "" > ${LOGFILE}

# Collect metadata
test_classpath=$(defects4j export -p cp.test)
src_classes_dir=$(defects4j export -p dir.bin.classes)
src_classes_dir="$CDIR/$BUGDIR/$src_classes_dir"
test_classes_dir=$(defects4j export -p dir.bin.tests)
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