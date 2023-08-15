containername="ghrb_verify"

docker ps -aq --filter "name=$containername" | grep -q . && docker stop $containername && docker rm $containername
docker run -dt --name $containername -v $(pwd)/data:/root/data -v $(pwd)/scripts:/root/scripts -v $(pwd)/results:/root/results -v $(pwd):/root/GHRB jylee/ghrb:latest
docker exec -it $containername /bin/bash