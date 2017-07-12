#! /bin/bash
threadNum=18
migrationItv=100
loop(){
  for i in `seq 0 9`
  do
    sh exe.sh $1 $2 ${threadNum} 4 ${migrationItv} $i 0 server1 server2 server3
  done
}
#kkk
loop 5 dataname1
loop 5 dataname2
