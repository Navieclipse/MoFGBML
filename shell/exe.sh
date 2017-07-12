#! /bin/bash
echo "start"
waitTime="$1"
dataName="$2"
threadNum="$3"
islandNum="$4"
migrationItv="$5"
cvNum="$6"
repNum="$7"
serverNum=`expr $# - 7`
dirName='/home/server/'
######################################################
#Stop server jars
function killServer() {
for i in `seq 0 $#`
do
echo "killer"
num=$(ssh client@${!i} "ps x | grep Server.jar | grep -v grep | cut -d ' ' -f0")
if test "$num" != ""; then
ssh client@${!i} kill $num && echo " ${!i} was killed" && continue
fi
num=$(ssh client@${!i} "ps x | grep Server.jar | grep -v grep | cut -d ' ' -f1")
if test "$num" != ""; then
ssh client@${!i} kill $num && echo " ${!i} was killed" && continue
fi
num=$(ssh client@${!i} "ps x | grep Server.jar | grep -v grep | cut -d ' ' -f2")
if test "$num" != ""; then
ssh client@${!i} kill $num && echo " ${!i} was killed" && continue
fi
num=$(ssh client@${!i} "ps x | grep Server.jar | grep -v grep | cut -d ' ' -f3")
if test "$num" != ""; then
ssh client@${!i} kill $num && echo " ${!i} was killed" && continue
fi
num=$(ssh client@${!i} "ps x | grep Server.jar | grep -v grep | cut -d ' ' -f4")
if test "$num" != ""; then
ssh client@${!i} kill $num && echo " ${!i} was killed" && continue
fi
num=$(ssh client@${!i} "ps x | grep Server.jar | grep -v grep | cut -d ' ' -f5")
if test "$num" != ""; then
ssh client@${!i} kill $num && echo " ${!i} was killed" && continue
fi
done
exit
}
######################################################
trap "killServer $8 $9 ${10} ${11}" EXIT
######################################################
echo "Wait data loading..."
for i in `seq 8 $#`
do
exist=`ssh client@${!i} "ps x | grep Server.jar | grep -v grep"`
if test "$exist" == ""; then
ssh client@${!i} "java -jar -Xmx20g ${dirName Server.jar ${dataName} ${dirName} 50001 ${threadNum} ${islandNum} ${cvNum} ${repNum}" &
fi
done
######################################################
sleep ${waitTime}
######################################################
ssh client@maitre "java -jar -Xmx8g ${dirName}client.jar 1 ${dataName} 1000 100 2 0 ${cvNum} ${repNum} 1993 true true ${dirName} ${serverNum} 50001 ${islandNum} ${migrationItv} $8 $9 ${10} ${11}" 
######################################################
sleep 5
echo "end"

