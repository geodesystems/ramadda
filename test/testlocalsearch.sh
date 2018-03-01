while [ 1 ] 
do
    time curl  -o /dev/null "http://localhost:8080/repository/search/do?search.type=search.type.text&search.submit.x=0&search.submit.y=0&text=data&provider=this&xoutput=json"
done 
