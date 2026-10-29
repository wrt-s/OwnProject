#!/bin/bash

appName=excel-app
docker-compose stop $appName
docker-compose rm -f $appName
docker rmi -f $appName
docker-compose build $appName
docker-compose up -d $appName

echo "发版成功！！"
sleep 3
docker logs -f $appName

