#!/bin/bash
# Prepare a blank EC2 instance for running a registry
# Installs required dependent packages
# Must be run sudo

ROOT=/var/local/registry
set -e

echo "** Installing nginx ..."
echo "**   download package ..."
yum install -y nginx.x86_64

if [ $(grep -c nginx /etc/logrotate.conf) -ne 0 ]
then
  echo "**   logrotate for nginx already configured"
else
  cat nginx.logrotate.conf >> /etc/logrotate.conf
  echo "**   logrotate for nginx configured"
fi

cp nginx.conf /etc/nginx/nginx.conf

echo "**   starting service ..."
service nginx start
chkconfig nginx on

echo "** Installing java and tomcat ..."
yum install -y java-1.7.0-openjdk-demo.x86_64 tomcat7.noarch
alternatives --set java /usr/lib/jvm/jre-1.7.0-openjdk.x86_64/bin/java

if [ $(java -version 2>&1 | grep 1.7. -c) -ne 1 ]
then
  echo "**   ERROR: java version doesn't look right, try manual alternatives setting restart tomcat7"
  echo "**   java version is:"
  java -version
fi

echo "**    starting tomcat"
service tomcat7 start
chkconfig tomcat7 on

echo "** configuring registry file root area ..."
if [ ! -d $ROOT ]
then
  echo "**   creating root directory"
  mkdir $ROOT
fi

if ! sudo -u tomcat touch $ROOT/test;
then
  echo "**   making root directory accessible to tomcat user"
  chown tomcat $ROOT
  chgrp tomcat $ROOT
else
  rm $ROOT/test
fi

if [ ! -d $ROOT/ui ]
then
  echo "**    creating ui area"
  mkdir $ROOT/ui
  chown tomcat $ROOT/ui
  chgrp tomcat $ROOT/ui
fi

if [ $(grep -c -e 'tomcat.*/var/local/registry/proxy-conf.sh' /etc/sudoers) -ne 0 ]
then
  echo "** sudoers already configured"
else
  cat sudoers.conf >> /etc/sudoers
  echo "** added sudoers access to proxy configuration"
fi

echo "** install web app"
cp ukl-registry*.war /var/lib/tomcat7/webapps/ROOT.war
echo "** DONE"
echo "Check startup progress with: sudo tail -f /var/log/tomcat7/catalina.out"
