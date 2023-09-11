CDIR=$(pwd)
GHRB_HOME="/root/framework"

PID="gson"
for BID in $(cut -f1 -d',' "/root/framework/commit_db/${PID}_bugs.csv"); do

if [ $BID == "bug_id" ]
    then
        continue
fi
BUGDIR="${PID}_${BID}"
echo ${BUGDIR}

./cli.py checkout -p ${PID} -v ${BID}b -w ${CDIR}/${BUGDIR}

#mkdir "${CDIR}/${PID}_${BID}"

cd "${CDIR}/${PID}_${BID}"

# Collect metadata
current_dir=$(pwd)
mvn clean compile -q
mvn test -Dmaven.test.failure.ignore=true -q
mvn dependency:build-classpath -DincludeScope=test -Dmdep.outputFile=${current_dir}/cp.txt -q
test_classpath=$(cat "${current_dir}/cp.txt")
src_classes_dir=$(mvn help:evaluate -Dexpression=project.build.outputDirectory -q -DforceStdout)
# src_classes_dir="$CDIR/$BUGDIR/$src_classes_dir"
test_classes_dir=$(mvn help:evaluate -Dexpression=project.build.testOutputDirectory -q -DforceStdout)
# test_classes_dir="$CDIR/$BUGDIR/$test_classes_dir"

echo "$PID-${BID}b's classpath: $test_classpath" >&2
echo "$PID-${BID}b's bin dir: $src_classes_dir" >&2
echo "$PID-${BID}b's test bin dir: $test_classes_dir" >&2

export GZOLTAR_CLI_JAR="$CDIR/gzoltar-1.7.3/lib/gzoltarcli.jar"

echo "$current_dir/gson/target/test-classes"
unit_tests_file="$CDIR/$BUGDIR/unit_tests.txt"
relevant_tests="*"
java -cp "$test_classpath:"$current_dir/gson/target/test-classes":$GHRB_HOME/junit-4.13.2.jar:$GZOLTAR_CLI_JAR" \
  com.gzoltar.cli.Main listTestMethods \
    "$current_dir/gson/target/test-classes" \
    --outputFile "$unit_tests_file" \
    --includes "$relevant_tests" # >> ${LOGFILE}
cd ..


done