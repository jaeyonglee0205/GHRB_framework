for i in {1..13}
do  
    rm -rf testing
    ./cli.py checkout -p checkstyle -v "$i"b -w /root/framework/testing
    ./cli.py compile -w /root/framework/testing
    ./cli.py test -w /root/framework/testing
done