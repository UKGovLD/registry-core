#!/bin/sh

for i in $(find .. -type f -name "*.java" | xargs grep -L 'http://www.apache.org/licenses/LICENSE-2.0');
do
  echo $i
  sed -e '/(c) Copyright/r doc/license-header' $i > temp.java
  mv temp.java $i
done


