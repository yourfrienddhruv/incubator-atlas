#!/bin/bash
bin=`dirname "${BASH_SOURCE-$0}"`
bin=`cd "$bin">/dev/null; pwd`

echo "Resetting Atlas"
"$bin"/atlas_stop.py
backupName=$(date +"%m-%d-%Y-%T")
echo "backing up current data to $backupName"
mv "$bin"/../zookeeper-data "$bin"/../zookeeper-data_backup-$backupName
echo
mv "$bin"/../root "$bin"/../root_backup-$backupName
"$bin"/atlas_start.py
#echo "waiting for 2 min to let atlas_server start, and then create entities, if next command fails then run manually.. $bin/quick_start.py"
#sleep 120s
#"$bin"/quick_start.py
