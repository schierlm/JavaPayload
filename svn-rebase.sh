#!/bin/sh -ex
cp svn.authors svn.authors~
git checkout svnmaster
mv svn.authors~ svn.authors
git svn rebase
rm svn.authors
git checkout master
git merge svnmaster
git push github svnmaster master
echo Done!