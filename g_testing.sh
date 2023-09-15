CDIR=$(pwd)
GHRB_HOME="/root/framework"

# for PID in $(./cli.py pid -q); do
PID="jackson-core"
for BID in $(cut -f1 -d',' "/root/framework/commit_db/${PID}_bugs.csv"); do

if [ $BID == "bug_id" ]
    then
        continue
fi

if [ $BID == "2" ]
  then
    break
fi
BUGDIR="${PID}_${BID}"
echo ${BUGDIR}

./cli.py checkout -p ${PID} -v ${BID}b -w ${CDIR}/${BUGDIR}

#mkdir "${CDIR}/${PID}_${BID}"

# cd "${CDIR}/${PID}_${BID}"

# Collect metadata
current_dir=$(pwd)
# mvn clean compile -q
# mvn test -Dmaven.test.failure.ignore=true -q
./cli.py compile -w ${CDIR}/${BUGDIR}
./cli.py test -w ${CDIR}/${BUGDIR}

# mvn dependency:build-classpath -DincludeScope=test -Dmdep.outputFile=${current_dir}/cp.txt -q
# test_classpath=$(cat "${current_dir}/cp.txt")
test_classpath=$(./cli.py export -p cp.test -w ${CDIR}/${BUGDIR})

# src_classes_dir=$(mvn help:evaluate -Dexpression=project.build.outputDirectory -q -DforceStdout)
src_classes_dir=$(./cli.py export -p dir.bin.classes -w ${CDIR}/${BUGDIR})
# src_classes_dir="$CDIR/$BUGDIR/$src_classes_dir"

test_classes_dir=$(./cli.py export -p dir.bin.tests -w ${CDIR}/${BUGDIR})
# test_classes_dir=$(mvn help:evaluate -Dexpression=project.build.testOutputDirectory -q -DforceStdout)
# test_classes_dir="$CDIR/$BUGDIR/$test_classes_dir"
./cli.py export -p test-classes -w ${CDIR}/${BUGDIR}

echo "$PID-${BID}b's classpath: $test_classpath" >&2
echo "$PID-${BID}b's bin dir: $src_classes_dir" >&2
echo "$PID-${BID}b's test bin dir: $test_classes_dir" >&2

export GZOLTAR_CLI_JAR="$CDIR/gzoltar/com.gzoltar.cli/target/com.gzoltar.cli-1.7.4-SNAPSHOT-jar-with-dependencies.jar"
export GZOLTAR_AGENT_JAR="$CDIR//gzoltar/com.gzoltar.agent.rt/target/com.gzoltar.agent.rt-1.7.4-SNAPSHOT-all.jar"

# echo "$current_dir/gson/target/test-classes"

cd "${CDIR}/${PID}_${BID}"
unit_tests_file="$CDIR/$BUGDIR/unit_tests.txt"
relevant_tests="*"

#echo "classpath: $test_classpath"
java -cp "$test_classpath:"$test_classes_dir":$GHRB_HOME/junit-4.13.2.jar:$GZOLTAR_CLI_JAR" \
  com.gzoltar.cli.Main listTestMethods \
    "$test_classes_dir" \
    --outputFile "$unit_tests_file" \
    --includes "$relevant_tests"  #>> ${LOGFILE}


# classes to do fl on?


loaded_classes_file="$CDIR/$BUGDIR/test-classes.txt"
normal_classes=$(cat "$loaded_classes_file" | sed 's/$/:/' | sed ':a;N;$!ba;s/\n//g')
inner_classes=$(cat "$loaded_classes_file" | sed 's/$/$*:/' | sed ':a;N;$!ba;s/\n//g')
classes_to_debug="$normal_classes$inner_classes"
echo $normal_classes
#echo "Likely faulty classes: $classes_to_debug" >&2

# run gzoltar



ser_file="$CDIR/$BUGDIR/gzoltar.ser"
java -XX:MaxPermSize=4096M -javaagent:$GZOLTAR_AGENT_JAR=destfile=$ser_file,buildlocation=$src_classes_dir,includes=$classes_to_debug,excludes="",inclnolocationclasses=false,output="FILE" \
  -cp "$src_classes_dir:$GHRB_HOME/junit-4.13.2.jar:$test_classpath:$GZOLTAR_CLI_JAR" \
  com.gzoltar.cli.Main runTestMethods \
    --testMethods "$unit_tests_file" \
    --collectCoverage  #>> ${LOGFILE}


# report
java -cp "$src_classes_dir:$GHRB_HOME/junit-4.13.2.jar:$test_classpath:$GZOLTAR_CLI_JAR" \
    com.gzoltar.cli.Main faultLocalizationReport \
      --buildLocation "$src_classes_dir" \
      --granularity "line" \
      --inclPublicMethods \
      --inclStaticConstructors \
      --inclDeprecatedMethods \
      --dataFile "$ser_file" \
      --outputDirectory "${CDIR}/${PID}_${BID}" \
      --family "sfl" \
      --formula "ochiai" \
      --metric "entropy" \
      --formatter "txt" || die "Generation of fault-localization report has failed!"

#mv ${LOGFILE} "${CDIR}/${BUGDIR}/sfl/txt"

cd ..


done
# done