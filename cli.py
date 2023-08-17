#!/usr/bin/python3.9

import os
import json
from os import path
from collections import Counter
from tqdm import tqdm

#from util import config, fix_build_env
import re
import subprocess as sp
import argparse
import pandas as pd

def call_info(pid, bid):

    '''
    Summary of configuration for Project: {pid}
    ------------------------------------------
        Script dir: .../framework
        Base dir:   ...
        Major root: .../major
        Repo dir:   .../project_repos
    ------------------------------------------
        Project ID: 
        Program:    (name)
        Buildfile:  .../framework/projects/{pid}/{pid}.build.xml
    ------------------------------------------
        Number of bugs: 
        Commit db:
    '''

    '''
    bug_id, revision.id.buggy, revision.id.fixed, report.id, report.url
    '''
    with open("project_id.json", "r") as f:
        project_id = json.load(f)
    
    if pid not in project_id.keys():
        output = "Wrong project id"
        return output
    
    owner = project_id[pid]["owner"]
    number_of_bugs = project_id[pid]["number_of_bugs"]
    commit_db = project_id[pid]["commit_db"]
    repo_path = project_id[pid]["repo_path"]


    output= (f'''
    Summary of configuration for Project: {pid}
    ------------------------------------------
        Repo dir:   {repo_path}
    ------------------------------------------
        Project ID: {pid}
        Program:    {owner}
        Buildfile:  .../framework/projects/{pid}/{pid}.build.xml
    ------------------------------------------
        No. of bugs:    {number_of_bugs}
        Commit db:      {commit_db}
    ''')

    

    if bid is not None:
        active_bugs = pd.read_csv(commit_db)
        with open(f"verified_bug/verified_bugs_{owner}_{pid}.json", "r") as f:
            extra_info = json.load(f)

        for b_id in bid:
            revision_id_fixed = active_bugs.loc[active_bugs['bug_id'] == int(b_id)]["revision.id.fixed"].values[0]
            revision_id_buggy = active_bugs.loc[active_bugs['bug_id'] == int(b_id)]["revision.id.buggy"].values[0]
            report_id = active_bugs.loc[active_bugs['bug_id'] == int(b_id)]["report.id"].values[0]
            report_url = active_bugs.loc[active_bugs['bug_id'] == int(b_id)]["report.url"].values[0]
            
            output += (f'''
    Summary for Bug: {pid}-{b_id}
    ------------------------------------------
        Revision ID (fixed version):
        {revision_id_fixed}
    ------------------------------------------
        Revision date (fixed version):
        {extra_info[report_id]['PR_createdAt']}
    ------------------------------------------
        Bug report id:
        {report_id}
    ------------------------------------------
        Bug report url:
        {report_url}
    ------------------------------------------
        Revision ID (buggy version):
        {revision_id_buggy}
    ------------------------------------------
        Root cause in triggering tests:
        ???????
    ------------------------------------------
        List of modified sources:
    ''')
            for modified_file in extra_info[report_id]['changed_tests']:
                output += '\t' + modified_file + '\n'
    return output

def call_checkout(pid, vid, dir, patch):
    with open("project_id.json", "r") as f:
        project_id = json.load(f)

    if pid not in project_id.keys():
        output = "Wrong project id"
        return output
    
    commit_db = project_id[pid]["commit_db"]
    repo_path = project_id[pid]["repo_path"]

    active_bugs = pd.read_csv(commit_db)

    bid = vid[:-1]
    version = vid[-1]

    commit = None
    
    output = "" 

    report_id = active_bugs.loc[active_bugs['bug_id'] == int(bid)]["report.id"].values[0]

    test_patch_dir = os.path.abspath(os.path.join('./test_diff/', f'{report_id}.diff'))

    if version == "b":
        #buggy version
        commit = active_bugs.loc[active_bugs['bug_id'] == int(bid)]['revision.id.buggy'].values[0]
    
    elif version == "f":
        #fixed version
        commit = active_bugs.loc[active_bugs['bug_id'] == int(bid)]['revision.id.fixed'].values[0]

    else:
        output = "Choose 'b' for buggy version, 'f' for fixed version"
        return output
    
    '''
    The working directory to which the buggy or fixed project version 
    shall be checked out. The working directory
    has to be either empty or a previously used working directory.

    ALL files in a previously used working directory are deleted prior 
    to checking out the requested project version.
    '''

    if dir is not None:
        working_dir = dir
    else:
        working_dir = repo_path

    if commit != None:

        sp.run(['git', 'reset', '--hard', 'HEAD'],
            cwd=working_dir, stdout=sp.DEVNULL, stderr=sp.DEVNULL)
        sp.run(['git', 'clean', '-df'],
            cwd=working_dir, stdout=sp.DEVNULL, stderr=sp.DEVNULL)

        # checkout to the buggy version and apply patch to the buggy version
        sp.run(['git', 'checkout', commit], cwd=working_dir,
            stdout=sp.DEVNULL, stderr=sp.DEVNULL)
        
        output += (f"Checking out {commit} to {working_dir}\n")
        
        if version == "b" and patch == True:
            sp.run(['git', 'apply', test_patch_dir], cwd=working_dir,
                stdout=sp.DEVNULL, stderr=sp.DEVNULL)

            output += (f"Applying patch\n")
        
        output += (f"Check out program version {pid}-{vid}\n")
    else:
        output += "Cannot find version..."
    
    return output

def find_env (pid):
    with open("project_id.json", "r") as f:
        project_id = json.load(f)

    if pid not in project_id.keys():
        output = "No matching project id"
        return output

    requirements = project_id[pid]["requirements"]
    if len(requirements["extra"]) != 0:
        extra = requirements["extra"]
    
    build = requirements["build"]
    jdk_required = requirements["jdk"]
    wrapper = requirements["wrapper"]
    # if requirements["gradle"] != "0":
    #     gradle_required = requirements["gradle"]

    mvn_required = None
    mvnw = False
    gradlew = False

    if build == "maven":
        if wrapper:
            mvnw = True
            print("Using Maven Wrapper")
        else:
            mvn_required = requirements["version"]
            print(f"Required Maven Version: {mvn_required}")
    elif build == "gradle":
        if wrapper:
            gradlew = True
            print("Using Gradle Wrapper")

    print(f"Required JDK Version: {jdk_required}")

    JAVA_HOME = mvn_path = None
    if jdk_required == '8':
        JAVA_HOME = '/usr/lib/jvm/java-8-openjdk-amd64'
    elif jdk_required == '11':
        JAVA_HOME = '/usr/lib/jvm/java-11-openjdk-amd64'
    elif jdk_required == '17':
        JAVA_HOME = '/usr/lib/jvm/java-17-openjdk-amd64'
    
    if mvn_required is None:
        mvn_path = None
    elif mvn_required == '3.8.6':
        mvn_path = '/opt/apache-maven-3.8.6/bin'
    elif mvn_required == '3.8.1':
        mvn_path = '/opt/apache-maven-3.8.1/bin'

    new_env = os.environ.copy()
    new_env['JAVA_HOME'] = JAVA_HOME
    if mvn_path is not None:
        new_env['PATH'] = os.pathsep.join([mvn_path, new_env['PATH']])

    return new_env, mvnw, gradlew

def call_compile(pid, dir):
    '''
    Docker:
        maven 3.8.1, 3.8.6
        jdk 8, 11, 17
        gradle 7.6.2
    
    '''
    output = ""
    with open("project_id.json", "r") as f:
        project_id = json.load(f)

    if pid not in project_id.keys():
        output = "No matching project id"
        return output

    repo_path = project_id[pid]["repo_path"]

    new_env, mvnw, gradlew = find_env(pid)

    if not mvnw and not gradlew:
        out = sp.run(['mvn', 'clean', 'compile'], env=new_env, stdout = sp.PIPE, stderr = sp.PIPE, check=True, cwd=repo_path)
    elif mvnw:
        out = sp.run(['./mvnw', 'clean', 'compile'], env=new_env, stdout=sp.PIPE, stderr=sp.PIPE, check=True, cwd=repo_path)
    
    elif gradlew:
        out = sp.run(['./gradlew', 'clean', 'compileJava'], env=new_env, stdout=sp.PIPE, stderr=sp.PIPE, check=True, cwd=repo_path)

    if "BUILD SUCCESS" in out.stdout.decode():
        output += "\033[92mBuild Success\033[0m"
    else:
        output += "\033[91mBuild Failed\033[0m"
    '''
    mvn clean install -DskipTests=true
    mvn clean package -Dmaven.buildDirectory='target'
    '''
    return output

def run_test (new_env, mvnw, gradlew, test_case, repo_path, command=None):

    output = ""
    if not mvnw and not gradlew:
        default = ['mvn', 'clean', 'test', f'-Dtest={test_case}', '-DfailIfNoTests=false', '-e']
        if command is not None:
            extra_command = command.split()
            new_command = default + extra_command
            run = sp.run(new_command,
                         env=new_env, stdout=sp.PIPE, stderr=sp.PIPE, cwd=repo_path)
        else:
            run = sp.run(default,
                        env=new_env, stdout=sp.PIPE, stderr=sp.PIPE, cwd=repo_path)

    elif mvnw:
        default = ['./mvnw', 'clean', 'test', f'-Dtest={test_case}']
        if command is not None:
            extra_command = command.split()
            new_command = default + extra_command

            run = sp.run(new_command,
                        env=new_env, stdout=sp.PIPE, stderr=sp.PIPE, cwd=repo_path)
        else:
            run = sp.run(default,
                        env=new_env, stdout=sp.PIPE, stderr=sp.PIPE, cwd=repo_path)
    elif gradlew:
        default = ["./gradlew", "test", "--tests", f'{test_case}']
        print("gradlew")
        if command is not None:
            if 'test' in command:
                new_command = ["./gradlew", command, '--tests', f'{test_case}']
                run = sp.run(new_command,
                             env=new_env, stdout=sp.PIPE, stderr=sp.PIPE, cwd=repo_path)
            else:
                run = sp.run(new_command,
                             env=new_env, stdout=sp.PIPE, stderr=sp.PIPE, cwd=repo_path)
        else:
            run = sp.run(default,
                         env=new_env, stdout=sp.PIPE, stderr=sp.PIPE, cwd=repo_path)
    '''
    Modify for gradle
    '''
    stdout = run.stdout.decode()
    if "BUILD SUCCESS" in stdout:
        output += (f'''
\033[1mTEST: {test_case}\033[0m 

\033[92mTest Success\033[0m
------------------------------------------------------------------------\n''')
    elif "There are test failures" in stdout:
        pattern = r'\[ERROR\] Failures:(.*?)\[INFO\]\s+\[ERROR\] Tests run:'
        match = re.search(pattern, stdout, re.DOTALL)
        fail_part = match.group(1).strip()
        fail_part = re.sub(r'\[ERROR\]', '', fail_part).strip()
        output += (f'''
\033[1mTEST: {test_case}\033[0m

\033[91mFailure info:\033[0m
    {fail_part}
------------------------------------------------------------------------\n''')
    
    return output

def call_test(pid, bid, dir, test_case, test_suite):
    '''
    default is the current directory
    test_case -> By default all tests are executed
    test_suite -> The archive file name of an external test suite. 
    '''
    output = ""
    with open("project_id.json", "r") as f:
        project_id = json.load(f)

    if pid not in project_id.keys():
        output = "No matching project id"
        return output

    commit_db = project_id[pid]["commit_db"]
    repo_path = project_id[pid]["repo_path"]

    owner = project_id[pid]["owner"]

    repo_name = owner + "_" + pid

    command = None

    if len(project_id[pid]['requirements']['extra']) != 0:
        command = project_id[pid]['requirements']['extra']['command']
    
    with open(f"verified_bug/verified_bugs_{repo_name}.json", "r") as f:
        verified_bugs = json.load(f)

    active_bugs = pd.read_csv(commit_db)
    report_id = active_bugs.loc[active_bugs['bug_id'] == int(bid)]['report.id'].values[0]
    
    target_tests = verified_bugs[report_id]["execution_result"]["success_tests"]

    new_env, mvnw, gradlew = find_env(pid)


    def find_test (input):
        for test in target_tests:
            if test.find(input) != -1:
                return test
            else:
                return None
            
    if test_case is not None:
        found_test_case = find_test(test_case)

        if found_test_case is None:
            print("External test case")
            output += run_test(new_env, mvnw, gradlew, test_case, repo_path, command)
        else:
            print("Internal test case")
            output += run_test(new_env, mvnw, gradlew, found_test_case, repo_path, command)
    elif test_suite is not None:
        print("External test suite")
        pass
    else:
        print("Running all test cases")
        for test in target_tests:
            output += run_test(new_env, mvnw, gradlew, test, repo_path, command)

    
    return output


def call_bid(pid, quiet):
    with open("project_id.json", "r") as f:
        project_id = json.load(f)

    if pid not in project_id.keys():
        output = "Wrong project id"
        return output
    
    number_of_bugs = project_id[pid]["number_of_bugs"]
    commit_db = project_id[pid]["commit_db"]
    output = ""
    if not quiet:
        output += f'''
    Bug Information for {pid}

    Total number of bugs: {number_of_bugs}
    ------------------------------------------
'''

    active_bugs = pd.read_csv(commit_db)

    for bug_id in active_bugs["bug_id"]:
        output += f"\t{bug_id}\n"
    
    return output

def call_pid():

    output = '''
    Owner:\t\tProject ID
    ----------------------------------------
''' 
    with open("project_id.json", "r") as f:
        project_id = json.load(f)
    
    for pid in project_id.keys():
        project_name = project_id[pid]["owner"]
        output += f"    {project_name}:\t\t{pid}\n"
    
    return output


if __name__ == '__main__':

    default_dir = os.getcwd()

    parser = argparse.ArgumentParser(description="Command-line Interface for GHRB")

    subparsers = parser.add_subparsers(dest="command")

    #  d4j-info -p project_id [-b bug_id]
    parser_info = subparsers.add_parser('info',
                                        help="View configuration of a specific project or summary of a specific bug")
    
    parser_info.add_argument('-p', dest="project_id", action="store",
                             help="The id of the project for which the information shall be printed")
    
    parser_info.add_argument('-b', dest='bug_id', action="store", type=int, nargs="*",
                             help="The id of the bug for which the information shall be printed. Format: \d+")
    

    #  d4j-checkout -p project_id -v version_id -w work_dir
    parser_checkout = subparsers.add_parser('checkout',
                                        help="Check out a buggy or a fixed project version")

    parser_checkout.add_argument("-p", dest="project_id", action="store",
                                 help="The id of the project for which the information shall be checked out")
    
    parser_checkout.add_argument("-v", dest="version_id", action="store",
                                 help="The version id that shall be checked out. Format: \d+[bf]")
    
    parser_checkout.add_argument("-w", dest="work_dir", action="store",
                                 help="The working directory to which the buggy or fixed project version shall be checked out")
    
    parser_checkout.add_argument("-pch", dest="patch", action="store", default=True,
                                 help="(Only for buggy versions) Checkout with/without patch")
    

    # d4j-compile -- compile a checked-out project version.
    parser_compile = subparsers.add_parser('compile',
                                        help="Compile sources and developer-written tests of a buggy or a fixed project version")
    
    parser_compile.add_argument("-p", dest="project_id", action="store",
                                help="The project id that shall be built")
    parser_compile.add_argument("-w", dest="work_dir", action="store", default=default_dir,
                                help="The working directory of the checked-out project version (optional). Default is the current directory")
    

    #  d4j-test [-w work_dir] [-r | [-t single_test] [-s test_suite]]
    parser_test = subparsers.add_parser('test',
                                        help="Run a single test method or a test suite on a buggy or a fixed project version")
    
    parser_test.add_argument("-p", dest="project_id", action="store",
                             help="The project id that shall be tested")
    
    parser_test.add_argument("-b", dest="bug_id", action="store",
                             help="The bug id that shall be tested")
    
    parser_test.add_argument("-w", dest="work_dir", action="store", default=default_dir,
                             help="The working directory of the checked-out project version (optional). Default is the current directory")
    
    parser_test.add_argument("-t", dest="single_test", action="store",
                             help="Only run this single test method (optional). By default all tests are executed. Format: <test_class>:<test_method>")
    
    parser_test.add_argument("-s", dest="test_suite", action="store",
                             help="The archive file name of an external test suite (optional)")
    
    #   d4j-bids -p project_id [-D|-A]

    parser_bid = subparsers.add_parser('bid',
                                       help="Print the list of available active bug IDs")
    
    parser_bid.add_argument("-p", dest='project_id', action="store",
                            help="The ID of the project for which the list of bug IDs is returned")
    
    parser_bid.add_argument("-q", "--quiet", dest='quiet', action='store_true',
                            help="Print only the bug IDs")
    
    
    parser_pid = subparsers.add_parser('pid',
                                       help="Print the list of available project IDs")
    

    
    
    args = parser.parse_args()
    #print(args)
    if args.command == "info":
        output = call_info(args.project_id, args.bug_id)
        print(output)
    elif args.command == "checkout":
        output = call_checkout(args.project_id, args.version_id, args.work_dir, args.patch)
        print(output)
    elif args.command == "compile":
        output = call_compile(args.project_id, args.work_dir)
        print(output)
    elif args.command == "test":
        output = call_test(args.project_id, args.bug_id, args.work_dir, args.single_test, args.test_suite)
        print(output)
    elif args.command == "bid":
        output = call_bid(args.project_id, args.quiet)
        print(output)
    elif args.command == "pid":
        output = call_pid()
        print(output)


    
