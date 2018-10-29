# file-renamer
### [![Link Shield](https://img.shields.io/badge/required-JDK%207-red.svg)](https://docs.oracle.com/javase/7/docs/api/java/nio/file/attribute/BasicFileAttributes.html)
```
Disclaimer : This program is made only for Linux, and has only been tested on Linux.
Required : JDK 7. You need the JDK for compiling the java files.
```
A program for renaming files automatically in a directory. This program monitors a directory (whose path is provided by the user) and, on noticing a change in the number of files, it automatically renames the latest file to the desired string (which is also provided by the user) followed by a number, which is tracked.

## Example
If the pattern (more accurately "prefix") provided by the user is `Test`, the first new file will be renamed as `Test 1`. Then, as long as the program continues to run in the background, every new file in that directory will be renamed by numbers. So the second new file will be `Test 2` and the third will be `Test 3` and so on.

## Instructions
* This program has been created for Linux.
* The program does not change names of directories.
* The program does not change names of hidden files (since otherwise, it would remove the dot before the files and they would not be hidden anymore).
* The program has three modes :
  * Normal mode (started as `renamer`)
	This mode starts numbering files with 1.
  * Continue mode (started as `renamer -c` or `renamer --continue`)
	This mode starts numbering the files with the number of files already present in the directory. So if there are 10 files already in the directory, the new file will be renamed as `%pattern% 11`.
  * Number mode (started as `renamer -n %num%` or `renamer --number %num%`)
	The `%num%` is where the user provides a custom starting point. Then, the renaming begins from the the custom point. So, if the `%num%` provided by the user is 10, the first new file will be renamed as `%pattern% 10`.
* Removes any empty files (Of length 0 bytes) it notices. Might be a **bug** for some users.
* To start monitoring the current directory open in terminal, use `"."`. For a subdirectory `Dir`, you can write like `./Dir`.
* To refer to the home directory, use `"~"`. For a subdirectory `Dir`, you can write like `~/Dir`.

## Installation
This program is installed user-wise. So you don't need to run anything as `sudo`, which is better anyways.
Open a terminal in this repository's directory. Then run the following commands:
```
chmod +x setup
./setup
```
To uninstall, run:
```
chmod +x uninstall
./uninstall
```
## Usage
Open a terminal in the directory you want to monitor for renaming. Then run the command:
```
renamer
```
The different modes explained above can be used like:
```
Default :       renamer
Continue :      renamer -c           OR     renamer --continue
Custom Number : renamer -n %num%     OR     renamer --number %num%
Help :          renamer -h           OR     renamer --help
```
After that, you will be asked:
```
Enter the path to directory to be monitored:
```
Here, enter the path to the directory, which you want to be monitored. Then, you will be asked:
```
Enter the prefix to be used:
```
Here, enter the prefix you want added to the file names. So, if you want your new files renamed as `Test 1` and `Test 2`, you only need to enter:
```
Test
```
Notice that there are no spaces.
That is it. Your new files in the directory will be renamed automatically, as long as the program in running. To stop the program press `Ctrl+C`.

## Known bugs
* Does not work well when moving files.
* Does not rename all files when copying multiple files because the last access time is not same for all. So user should keep that in mind. If you need to copy multiple files, copy them one by one.