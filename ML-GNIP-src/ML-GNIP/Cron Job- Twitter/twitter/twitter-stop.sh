#!/bin/bash
# Grabs and kill a process from the pidlist that has the word myapp

pid=`ps ax | grep ML-GNIP-twitter`
kill $pid