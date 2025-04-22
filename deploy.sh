#!/bin/sh
# HOW TO RUN:
# deployharo.sh [versionNumber]
echo "Deploying canucksbot-$1.jar from '/ftp/files'"
echo "Stopping Service..."
/bin/systemctl stop canucksbot
echo "Moving .jar..."
mv /ftp/files/canucksbot-$1.jar /discord-bots/canucksbot/builds/canucksbot-$1.jar
echo "Linking build .jar..."
ln -sf /discord-bots/canucksbot/builds/canucksbot-$1.jar /discord-bots/canucksbot/builds/current.jar
echo "Starting Service..."
/bin/systemctl start canucksbot
echo "Done!"
