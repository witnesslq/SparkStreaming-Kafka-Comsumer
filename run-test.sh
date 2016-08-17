#!/usr/bin/env bash

cd ~/dev_pro/dw-realtime/realtime-sourcedata/

git pull origin dev

mvn clean package

scp ~/dev_pro/dw-realtime/realtime-sourcedata/target/realtime-souredata-1.0-SNAPSHOT-jar-with-dependencies.jar hadoop@spark001.jp:/home/hadoop/users/gongzi/

cd ~/dev_pro/dw-realtime/pathlist/

#git pull origin dev

mvn clean package

scp ~/dev_pro/dw-realtime/pathlist/target/pathlist-1.0-SNAPSHOT-jar-with-dependencies.jar hadoop@spark001.jp:/home/hadoop/users/gongzi/


if test $? -ne 0
then
echo "spark failed!"
exit 2
fi