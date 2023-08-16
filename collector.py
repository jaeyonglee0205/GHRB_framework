import os
import re
import glob
import json
from os import path
from collections import Counter, defaultdict
from tqdm import tqdm

import subprocess as sp
import javalang
import argparse
import shutil
import pandas as pd


def project_id_collector():
    project_map = defaultdict(dict)
    
    for file in os.listdir("verified_bug/"): 
        project_id = file.split("_")[-1].replace(".json", "")
        owner = file.split("_")[-2]
        
        with open("verified_bug/" + file, 'r') as f:
            active_bug_list = json.load(f)
        
        number_of_bugs = len(active_bug_list)
        commit_db = f"commit_db/{project_id}_bugs.csv"

        with open("requirements.json", 'r') as f:
            requirements = json.load(f)

        repo_path = None

        '''
        Temporary code for repo_path
        !!!Need to change!!!
        '''

        for dir in os.listdir("../repos/"):
            if project_id in dir:
                repo_path = os.path.abspath(os.path.join(
                    '../repos/', f'{dir}'
                ))
        if repo_path is None:
            for dir in os.listdir("../paper_repos/"):
                if project_id in dir:
                    repo_path = os.path.abspath(os.path.join(
                        '../paper_repos/', f'{dir}'
                    ))
        project_map[project_id]["owner"] = owner
        project_map[project_id]["number_of_bugs"] = number_of_bugs
        project_map[project_id]['commit_db'] = commit_db
        project_map[project_id]['repo_path'] = repo_path
        project_map[project_id]['requirements'] = requirements[project_id]

    
    with open('project_id.json', 'w') as f:
        json.dump(project_map, f, indent=2)

def commit_db_collector():
    for file in os.listdir("verified_bug/"): 
        project_id = file.split("_")[-1].replace(".json", "")
        owner = file.split("_")[-2]
        
        with open("verified_bug/" + file, 'r') as f:
            active_bug_list = json.load(f)
        
        commit_db = pd.DataFrame(columns=['bug_id', 'revision.id.buggy', 'revision.id.fixed', 'report.id', 'report.url'], 
                                 index=list(range(len(active_bug_list))))

        commit_db = commit_db.fillna(0)

        commit_db.index += 1

        index = 1
        for key, value in active_bug_list.items():
            commit_db.loc[index] = [index, value['buggy_commit'], value['merge_commit'], value["bug_id"], value['issue']['url']]
            index += 1
        
        commit_db.to_csv(os.getcwd() + "/commit_db/" + project_id + "_bugs.csv", index=False)

def find_og_collector():
    total_list = set()
    filtered = []
    with open('og_mapping.json', 'r') as f:
        og_mapping = json.load(f)
    
    for key, value in og_mapping.items():
        total_list.add(key)

    for file in os.listdir("verified_bug/"):
        with open("verified_bug/" + file, 'r') as f:
            active_bug_list = json.load(f)
        for key in active_bug_list.keys():
            #print(key)
            project_name = key.split('-')[0]
            bug_id = key.split('-')[1]
            if key in total_list:
                filtered.append(key)

            elif project_name == 'assertj_assertj' and "".join(project_name + '-core-' + bug_id) in total_list:
                filtered.append(key)
                    
    print(total_list)
    print(len(total_list))
    print(filtered)
    print(len(filtered))

def find_total_bug():
    bug_dict = defaultdict(int)
    total = 0
    for file in os.listdir("verified_bug/"):
        with open("verified_bug/" + file, 'r') as f:
            active_bug_list = json.load(f)
        name = str(file).replace(".json", '')
        bug_dict[name.split("_")[-1]] = len(active_bug_list)
        total += len(active_bug_list)
    
    print(bug_dict)
    print('total: ', total)

if __name__ == '__main__':

    project_id_collector()
    commit_db_collector()
    find_og_collector()
    find_total_bug()
    