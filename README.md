# GHRB_framework
```
cd Docker
docker build -t ghrb_framework .
```

```
sh run_docker_container.sh
```

```
cd framework/Docker
pip install -r requirements.txt  
cd ..
```

```
chmod +x cli.py

cd debug
python collector.py
```

Check whether cli.py runs correctly  
`./cli.py -h`  

Currently, all directory path needs to be in absolute path inside the docker container  

Example:  

```
./cli.py checkout -p gson -v 1b -w /root/framework/testing
./cli.py compile -w /root/framework/testing
./cli.py test -w /root/framework/testing
```

'grpc_grpc-java: 21', 'powermock_powermock: 1', 'spring-cloud_spring-cloud-netflix: 6', 'cucumber_cucumber-jvm: 15', 'immutables_immutables: 2', 'swagger-api_swagger-core: 5', 'bitcoinj_bitcoinj: 2', 'parse-community_Parse-SDK-Android: 6', 'google_guice: 1', 'auth0_java-jwt: 3', 'gephi_gephi: 2', 'OpenTSDB_opentsdb: 1', 'javaparser_javaparser: 60', 'apache_shardingsphere-elasticjob: 18', 'google_auto: 1', 'knowm_XChange: 1', 'failsafe-lib_failsafe: 1', 'junit-team_junit5: 23', 'dropwizard_metrics: 5', 'spring-projects_spring-security: 34', 'googlemaps_android-maps-utils: 2', 'chewiebug_GCViewer: 1', 'crate_crate: 187', 'hazelcast_hazelcast: 135'