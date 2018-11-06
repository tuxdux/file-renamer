import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.concurrent.*;
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
 * 1. This program is created for Linux.
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
 * 	     time is not same for all. So user should keep that in mind. If you need to copy
 	     multiple files, copy them one by one. If anyone can fix this, it would be greatly
 	     appreciated.
 * [#] Feature : Removes any empty files (Of length 0 bytes) it notices. 
 * 		  Might be a bug for some users.
 * [#] Feature : To start monitoring the current directory open in terminal, use '.'
 * [#] Feature : To refer to the home directory, use '~'
 */
public class FileRename
{
	boolean toContinue;
	int earlierCount;
	int nameNumber;
	//-----------------These are all the special extensions-----------------
	//The extensions which have actually two extensions. You can add more.
	String[] specialExtensions = {".tar.gz",".tar.xz",".tar.bz2",".tar.bz"};
	//----------------------------------------------------------------------
	public FileRename(int initialCount, boolean toContinue, int nameNumber)
	{
		earlierCount = initialCount;
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
		Scanner scan = new Scanner(System.in);
		if(args.length>0)
		{
			//If the user wants to continue renaming files with the first
			//number being the number of files in the directory
			if(args[0].equals("-c") || args[0].equals("--continue"))
			{
				toContinue = true;
			}
			//If the user wants to continue renaming files with the first
			//number being an arbitrary number of his choice
			else if(args[0].equals("-n") || args[0].equals("--number"))
			{
				//If the user gave the -n option, they need to give the
				//starting number as argument too.
				//If they did not give any second argument
				if(args.length<2)
				{
					System.out.println("[!] Error : Enter the starting number "+
							"as an argument as well");
					System.exit(0);
				}
				String num = args[1];
				try
				{
					nameNum = Integer.parseInt(num);
				}
				//If they did not give a number, but a string
				catch(NumberFormatException e)
				{
					System.out.println("[!] Error : The starting number does not "+
							"seem to be a number");
					System.exit(0);
				}
			}
			else if(args[0].equals("-h") || args[0].equals("--help"))
			{
				printUsage();
				System.exit(0);
			}
			else
			{
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
				path = path.replace("~",user);
			}
			//If the current directory is to be used
			else if(path.charAt(0)=='.')
			{
				//Get the directory from which this instance was launched
				String user = System.getProperty("user.dir");
				//And set the path as that
				path = user;
			}
			scan = new Scanner(System.in);
			System.out.println("Enter the prefix to be used:");
			String pattern = scan.nextLine();
			
			File directory = new File(path);

			//The initial number of files in the directory
			int initialCount = directory.listFiles().length;
			FileRename renaming = new FileRename(initialCount, toContinue, nameNum);
			//The scheduler service
			final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
			executorService.scheduleAtFixedRate(new Runnable()
					{
						@Override
						public void run()
						{
							try
							{
								renaming.start(directory, pattern);
							}
							catch(Exception e)
							{
								e.printStackTrace();
							}
						}
					}, 0, 1, TimeUnit.SECONDS);
		}
		catch(Exception e)
		{
			//Output error messages with most common mistakes
			System.out.println("[!] Uh-oh. Something went wrong.");
			System.out.println("[!] Check that the directory exists.");
			System.out.println("[!] You do not need to use escape characters or quotes to input spaces.");
		}
	}
	public void start(File directory, String pattern) throws IOException
	{
		File[] files = directory.listFiles();
		if(files==null)
		{
			return;
		}
		//If there are new files in the directory
		if(files.length > earlierCount)
		{
			//Store the current file count in the earlierCount variable
			earlierCount = files.length;
			
			//----------To get the creation time of first file for comparison----------//
			File latestFile = files[0];
			BasicFileAttributes first = Files.readAttributes(latestFile.toPath(),
					BasicFileAttributes.class);
			//Note that we get the last access time in case the files are copied.
			FileTime latestTime = first.lastAccessTime();
			//------------------------------------------------------------------------//

			for(File currentFile : files)
			{
				BasicFileAttributes current = Files.readAttributes(currentFile.toPath(),
						BasicFileAttributes.class);
				FileTime currentTime = current.lastAccessTime();
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
			if(!latestFile.isDirectory() && !latestFile.isHidden())
			{
				//Get the initial size of the file
				long size = getSize(latestFile);
				//Check if the file is still changing (copying, downloading, etc).
				while(true)
				{
					try
					{
						Thread.sleep(100);
						if(getSize(latestFile)==size)
						{
							break;
						}
						size = getSize(latestFile);
					}
					catch(Exception e)
					{
						break;
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
				//If we have to continue renaming
				if(toContinue)
				{
					File newFile = null;
					if(pattern.equals(""))
						newFile = new File(directory.getPath()+File.separator+
								files.length+extension);
					else
						newFile = new File(directory.getPath()+File.separator+
                                                        pattern+" "+files.length+extension);
					latestFile.renameTo(newFile);
				}
				else
				{
					File newFile = null;
					if(pattern.equals(""))
						newFile = new File(directory.getPath()+File.separator+
								nameNumber+extension);
					else
						newFile = new File(directory.getPath()+File.separator+
							pattern+" "+nameNumber+extension);
					latestFile.renameTo(newFile);
					//Increment the number in the name by one
					nameNumber++;
				}
			}
		}
		//Make the earlier count equal to the number of files regardless of the above condition
		earlierCount = directory.listFiles().length;////
		//Now, there was a bug which creates another file of 0 bytes (never came to know why) if
		//something is being downloaded. Perhaps it was because of the .part file from firefox.
		//To fix that, we search for a file of 0 bytes. If there is such file, but is not a hidden
		//file, we delete it. Because who needs empty files anyways.
		//The hidden file precaution was taken because if the home directory was being monitored,
		//the .bash_history file was getting deleted sometimes.
		for(File file : files)
		{
			//If the length of file is 0 but the file is not a hidden file, delete it
			if(getSize(file)==0 && !file.isHidden())
			{
				//If the 0 byte file was renamed (by mistake), adjust
				//the next number for renaming
				if(file.getName().contains(pattern))
					nameNumber--;
				file.delete();
			}
		}
	}
	private long getSize(File file)
	{
		return file.length();
	}
}