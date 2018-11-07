import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
/**
 * A program for renaming files automatically in a directory.
 * This program monitors a directory (whose path is provided by the user) and,
 * on noticing a change in the number of files, it automatically renames the latest
 * file to the desired string (which is also provided by the user).
 * [Example]:
 * If the pattern (more accurately "prefix") provided by the user is "Test", the file will
 * be renamed as "Test 1". Then, as long as the program continues to run in the background,
 * every new file will be renamed by numbers. So the second new file will be "Test 2" and the
 * third will be "Test 3" and so on.
 * [Instructions]:
 * 1. This program has been tested only on Linux.
 * 2. Requires Java 7, since it uses the nio library.
 * 3. The program does not change names of directories.
 * 4. The program does not change names of hidden files.
 * 5. The program has three modes :
 *    - Normal mode (started as "renamer")
 *    	This mode starts numbering files with 1.
 *    - Continue mode (started as "renamer -c" or "renamer --continue")
 *      This mode starts numbering the files with the number of files already present
 *      in the directory. So if there are 10 files already in the directory, the new file
 *      will be renamed as %pattern% 11.
 *    - Number mode (started as "renamer -n %num%" or "renamer --number %num%")
 *    	The %num% is where the user provides a custom starting point. Then, the renaming
 *    	begins from the the custom point.
 * [!] Bug : Does not work well when moving files.
 * [!] Bug : Does not rename all files when copying multiple files because the last access
 * 	     time is same for all. So user should keep that in mind. If you need to copy
 *       multiple files, copy them one by one. If anyone can fix this, it would be greatly
 *       appreciated.
 * [#] Feature : To start monitoring the current directory open in terminal, use '.'
 * [#] Feature : To refer to the home directory, use '~'
 */
public class FileRename
{
    private boolean toContinue;
    private boolean filesChanged;
    private int initialCount;
    private int nameNumber;
    //-----------------These are all the special extensions-----------------
    //The extensions which have actually two extensions. You can add more.
    private String[] specialExtensions = {".tar.gz",".tar.xz",".tar.bz2",".tar.bz"};
    //----------------------------------------------------------------------
    private FileRename(int initialCount, boolean toContinue, int nameNumber)
    {
        this.filesChanged = false;
        this.initialCount = initialCount;
        this.nameNumber = nameNumber;
        this.toContinue = toContinue;
    }
    private static void printUsage()
    {
        System.out.println("Options:");
        System.out.println("                             Default. Start renaming files from 1.");
        System.out.println(" -c         --continue       Continue renaming files from already present.");
        System.out.println(" -n %num%   --number %num%   Start renaming files from %num%.");
        System.out.println(" -h         --help           Print this help message.");
    }
    public static void main(String[] args)
    {
        int nameNum = 1;
        boolean toContinue = false;
        Scanner scan;
        if(args.length>0)
        {
            //If the user wants to continue renaming files with the first
            //number being the number of files in the directory
            switch (args[0]) {
                case "-c":
                case "--continue":
                    toContinue = true;
                    break;
                //If the user wants to continue renaming files with the first
                //number being an arbitrary number of his choice
                case "-n":
                case "--number":
                    //If the user gave the -n option, they need to give the
                    //starting number as argument too.
                    //If they did not give any second argument
                    if (args.length < 2) {
                        System.out.println("[!] Error : Enter the starting number " +
                                "as an argument as well");
                        System.exit(0);
                    }
                    String num = args[1];
                    try {
                        nameNum = Integer.parseInt(num);
                    }
                    //If they did not give a number, but a string
                    catch (NumberFormatException e) {
                        System.out.println("[!] Error : The starting number does not " +
                                "seem to be a number");
                        System.exit(0);
                    }
                    break;
                case "-h":
                case "--help":
                    printUsage();
                    System.exit(0);
                default:
                    System.out.println("INVALID INPUT.");
                    printUsage();
                    System.exit(0);
            }
        }
        try
        {
            scan = new Scanner(System.in);
            //Get the required string values
            System.out.println("Enter the path to directory to be monitored:");
            String path = scan.nextLine();
            //If the user tried to access the home directory with '~'(tittle?) symbol
            if(path.charAt(0)=='~')
            {
                //Get the user's home directory
                String user = System.getProperty("user.home");
                //and replace '~' with the path to home directory
                path = path.substring(1);
                path = user+path;
            }
            //If the current directory is to be used
            else if(path.charAt(0)=='.')
            {
                //Get the directory from which this instance was launched
                path = System.getProperty("user.dir");
            }
            scan = new Scanner(System.in);
            System.out.println("Enter the prefix to be used:");
            String pattern = scan.nextLine();

            File directory = new File(path);

            //The initial number of files in the directory
            int initialCount = 0;
            File[] newFiles = directory.listFiles();
            if(newFiles!=null)
            {
                initialCount = newFiles.length;
            }
            FileRename renaming = new FileRename(initialCount, toContinue, nameNum);

            //----------------------The actual loop. This will never terminate.------------------------//
            while(true)
            {
                renaming.start(directory, pattern);
            }
            //------------------------------------------------------------------------------------//
        }
        catch(Exception e)
        {
            //Output error messages with most common mistakes
            System.out.println("[!] Uh-oh. Something went wrong.");
            System.out.println("[!] Check that the directory exists.");
            System.out.println("[!] You do not need to use escape characters or quotes to input spaces.");
        }
    }
    private void start(File directory, String pattern)
    {
        File[] files = directory.listFiles();
        if(files==null)
        {
            return;
        }
        if(files.length!=initialCount)
        {
            filesChanged = true;
        }
        if(!filesChanged)
        {
            return;
        }

        if(files.length==0)
        {
            return;
        }

        //----------To get the creation time of first file for comparison----------//
        File latestFile = files[0];
        FileTime latestTime = getTime(latestFile);
        if(latestTime==null)
        {
            return;
        }
        //------------------------------------------------------------------------//

        for(File currentFile : files)
        {
            FileTime currentTime = getTime(currentFile);
            if(currentTime==null)
            {
                return;
            }
            //If the current file was created after the previously stored
            //latest file, make it the latest file and also adjust the stored
            //latest time.
			/*Note : The value is greater than 0 if a the currentTime is
				 after the latestTime, i.e, the currentFile was created
				 after the latestFile.
			*/
            if(currentTime.compareTo(latestTime)>0)
            {
                latestTime = currentTime;
                latestFile = currentFile;
            }
        }
        //Now we have the latest file.
        //If the file is not a directory and is not a hidden file,
        //we rename it according to the provided pattern.
        if(!latestFile.isDirectory() && !latestFile.isHidden() &&
                !latestFile.getName().contains(pattern+" ") && latestFile.exists()
                && getSize(latestFile)!=0)
        {
            //Get the initial size of the file
            long prevSize = 0;
            long size = getSize(latestFile);
            //Check if the file is still changing (copying, downloading, etc).
            while(true)
            {
                try
                {
                	//Sleeping for 300 milliseconds. This is because if, after this
                	//interval, the file size has not changed, it means the file has
                	//completely downloaded, or copied. Unless, of course, you have
                	//a VERY slow internet. :)
                    TimeUnit.MILLISECONDS.sleep(300);
                    if(size==prevSize)
                    {
                        break;
                    }
                    prevSize = size;
                    size = getSize(latestFile);
                }
                catch(Exception e)
                {
                    System.out.println("[!] Unexpected Error.");
                }
            }
            //Get extension of file.
            String name = latestFile.getName();
            int dot = name.lastIndexOf('.');
            String extension = "";
            if(dot>=0)
            {
                extension = name.substring(name.lastIndexOf('.'));
            }
            //Also check if the file is of a special type.
            for(String specialExtension : specialExtensions)
            {
                if(name.endsWith(specialExtension))
                {
                    extension = specialExtension;
                    break;
                }
            }
            //Now rename the file.
            File newFile;
            //If we have to continue renaming
            //If pattern is not blank, make it so that a space is added
            //between the pattern and number in the name.
            if(!pattern.equals(""))
            {
                pattern = pattern+" ";
            }
            //Now to create the new file, to which the file has to be renamed.
            if(toContinue)
            {
                newFile = new File(directory.getPath()+File.separator+
                        pattern+files.length+extension);
            }
            else
            {
                newFile = new File(directory.getPath()+File.separator+
                        pattern+nameNumber+extension);
            }
            //Rename the actual file.
            try
            {
                boolean renamed = latestFile.renameTo(newFile);
                //Increment the number in the name by one
                if(renamed)
                {
                    nameNumber++;
                }
            }
            catch(SecurityException e)
            {
                //The file is still being written.
            }
        }
    }
    private FileTime getTime(File file)
    {
        FileTime time;
        try
        {
            BasicFileAttributes attribs = Files.readAttributes(file.toPath(),
                    BasicFileAttributes.class);
            time = attribs.lastAccessTime();
        }
        catch(IOException e)
        {
            return null;
        }
        return time;
    }
    private long getSize(File file)
    {
        return file.length();
    }
}