for i in {1..2}
do  
    rm -rf testing
    ./cli.py checkout -p seata -v "$i"b -w /root/framework/testing
    ./cli.py compile -w /root/framework/testing
    ./cli.py test -w /root/framework/testing
done