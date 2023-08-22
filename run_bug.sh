for i in {1..17}
do  
    rm -rf testing
    ./cli.py checkout -p rocketmq -v "$i"b -w /root/framework/testing
    ./cli.py compile -w /root/framework/testing
    ./cli.py test -w /root/framework/testing
done