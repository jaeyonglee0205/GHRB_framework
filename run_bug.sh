for i in {1..31}
do  
    rm -rf testing
    ./cli.py checkout -p javaparser -v "$i"b -w /root/framework/testing
    ./cli.py compile -w /root/framework/testing
    ./cli.py test -w /root/framework/testing
done