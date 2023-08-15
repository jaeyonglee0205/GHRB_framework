#!/usr/bin/python3

import os
import re
import glob
import json
from os import path
from collections import Counter
from tqdm import tqdm

#from util import config, fix_build_env

import subprocess as sp
import javalang
import argparse
import shutil
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
            b = b_id - 1
            revision_id_fixed = active_bugs.loc[b]["revision.id.fixed"]
            report_id = active_bugs.loc[b]["report.id"]
            report_url = active_bugs.loc[b]["report.url"]
            
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
        Root cause in triggering tests:
        ???????
    ------------------------------------------
        List of modified sources:
        {extra_info[report_id]['changed_tests']}
    ''')
    return output

def call_checkout(pid, vid, dir):
    with open("project_id.json", "r") as f:
        project_id = json.load(f)

    commit_db = project_id[pid]["commit_db"]
    repo_path = project_id[pid]["repo_path"]
    active_bugs = pd.read_csv(commit_db)

    bid = vid[:-1]
    version = vid[-1]

    commit = None
    
    output = "" 

    test_patch_dir = None
    
    if version == "b":
        #buggy version
        commit = active_bugs[bid]['revision.id.buggy']
    
    elif version == "f":
        #fixed version
        commit = active_bugs[bid]['revision.id.fixed']

    if commit != None:

        sp.run(['git', 'reset', '--hard', 'HEAD'],
            cwd=repo_path, stdout=sp.DEVNULL, stderr=sp.DEVNULL)
        sp.run(['git', 'clean', '-df'],
            cwd=repo_path, stdout=sp.DEVNULL, stderr=sp.DEVNULL)

        # checkout to the buggy version and apply patch to the buggy version
        sp.run(['git', 'checkout', commit], cwd=repo_path,
            stdout=sp.DEVNULL, stderr=sp.DEVNULL)
        
        output += (f"Checking out {commit} to {dir}\n")
        
        if version == "b":
            sp.run(['git', 'apply', test_patch_dir], cwd=repo_path,
                stdout=sp.DEVNULL, stderr=sp.DEVNULL)

            output += (f"Applying patch\n")
        
        output += (f"Check out program version-{pid}-{vid}\n")
    else:
        output += "Cannot find version..."
    
    return output

def call_compile(dir):
    pass

def call_test(dir, test_case, test_suite):
    '''
    default is the current directory
    test_case -> By default all tests are executed
    test_suite -> The archive file name of an external test suite. 
    '''
    pass

def call_bid(pid):
    with open("project_id.json", "r") as f:
        project_id = json.load(f)

    number_of_bugs = project_id[pid]["number_of_bugs"]
    commit_db = project_id[pid]["commit_db"]
    
    output = f'''
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
    Owner:\tProject ID
    -------------------------
''' 
    with open("project_id.json", "r") as f:
        project_id = json.load(f)
    
    for pid in project_id.keys():
        project_name = project_id[pid]["owner"]
        output += f"    {project_name}:\t{pid}\n"
    
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
    

    # d4j-compile -- compile a checked-out project version.
    parser_compile = subparsers.add_parser('compile',
                                        help="Compile sources and developer-written tests of a buggy or a fixed project version")
    
    parser_compile.add_argument("-w", dest="work_dir", action="store", default=default_dir,
                                help="The working directory of the checked-out project version (optional). Default is the current directory")
    

    #  d4j-test [-w work_dir] [-r | [-t single_test] [-s test_suite]]
    parser_test = subparsers.add_parser('test',
                                        help="Run a single test method or a test suite on a buggy or a fixed project version")
    
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
    
    
    parser_pid = subparsers.add_parser('pid',
                                       help="Print the list of available project IDs")
    

    
    
    args = parser.parse_args()
    #print(args)
    if args.command == "info":
        output = call_info(args.project_id, args.bug_id)
        print(output)
    elif args.command == "checkout":
        call_checkout(args.project_id, args.version_id, args.work_dir)
    elif args.command == "compile":
        call_compile(args.work_dir)
    elif args.command == "test":
        call_test(args.work_dir, args.single_test, args.test_suite)
    elif args.command == "bid":
        output = call_bid(args.project_id)
        print(output)
    elif args.command == "pid":
        output = call_pid()
        print(output)


    
