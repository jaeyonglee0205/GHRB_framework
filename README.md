# GHRB_framework
```
cd Docker
docker build -t ghrb_framework .
```

```
cd framework/Docker
pip install -r requirements.txt  
cd ..
```

```
sh run_docker_container.sh
```

```
chmod +x cli.py
python collector.py
```

Check whether cli.py runs correctly  
'./cli.py -h'  

Currently, all directory path needs to be in absolute path inside the docker container  

Example:  

```
./cli.py checkout -p gson -v 1b -w /root/framework/testing
./cli.py compile -w /root/framework/testing
./cli.py test -w /root/framework/testing
```