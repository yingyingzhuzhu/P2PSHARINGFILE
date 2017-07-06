package FileProcess;

import java.io.*;
import java.util.*;

public class FileProcess {
	//file name
	private static String FileName;
	//file path
	private static String inputFilePath;
	private static String outputFilePath;
	//size of every parts of the file
	private static int  FileSize;
	
	// use to remember how many parts we have
	static int NumberOfParts = 0;
	
	
	
	//init()
	/**
	 * FileName is the Name of the File e.g movie.avi
	 * inputFilePath is the path of the input file (DO NOT CONTAIN THE FILE NAME) e.g D:\\inputTest\\  linux is //
	 * Warning! When reassemble the file, you must remove the original file from the dir!
	 * outputFilePath is the path of the reassemble file (DO NOT CONTAIN THE FILE NAME) e.g D:\\ouputTest\\
	 * FileSize is the size of the parts e.g 1048576 (1MB, 1024*1024KB)
	 */
	public FileProcess(String FileName, String inputFilePath, String outputFilePath, int FileSize)
	{
		this.FileName = FileName;
		
		this.inputFilePath = inputFilePath;
		
		this.outputFilePath = outputFilePath;
		
		this.FileSize = FileSize;	
	}
	
	
	//split the file
	/**
	 * Split the input file
	 */
	public static void split() throws IOException
    {	
        FileInputStream FI = new FileInputStream(inputFilePath + FileName);
        FileOutputStream FO = null;
        
        //split the file into FileSize size
        byte[] buf = new byte[FileSize];
        
        int len = 0;
        
        //every time read a new buf (size is filesize) and output it until the file is empty
        while((len=FI.read(buf))!=-1)
        {
        	System.out.println("Size of this part is " + len + " byte");
        	FO = new FileOutputStream(outputFilePath + FileName + NumberOfParts + ".part");
        	System.out.println("File" + FileName + NumberOfParts + ".part is created" );
        	NumberOfParts =  NumberOfParts + 1;
        	//output the buf from buf[0] to buf[len]
        	FO.write(buf,0,len);
        	FO.flush();
        	FO.close();
        }
        FI.close();
        //NumberOfParts = NumberOfParts;
        
        System.out.println("File is splited into " + NumberOfParts + " parts" );
    }
	
	//reassemble the file
	/**
	 * Reassemble the file.
	 */
	public static void reassemble(int parts)throws IOException
    {
        FileOutputStream FO = null;
        
        FO = new FileOutputStream(outputFilePath + FileName);
        
        ArrayList<FileInputStream> array = new ArrayList<FileInputStream>();
        
        int count = 0;
        
        //Get every file in inputFilePath.  Remember to delete the files cannot combine!!!!
        for(int i = 0; i< parts; i++)
        {
        	File file = new File(inputFilePath + FileName + i + ".part");//利用File遍历文件夹下的文件
        	array.add(new FileInputStream(file));
        }

        //= =  ...  ArrayList本身没有枚举方法，通过迭代器来实现
        final Iterator<FileInputStream> it = array.iterator();
        Enumeration<FileInputStream>  en = new Enumeration<FileInputStream>()//匿名内部类，复写枚举接口下的两个方法
        {
            public boolean hasMoreElements(){
                return it.hasNext();
            }
            public FileInputStream nextElement()
            {
                return it.next();
            }
            
        };
        
        
        SequenceInputStream SI = new SequenceInputStream(en);
        byte[] buf = new byte[FileSize];
        while((count=SI.read(buf))!=-1)
        {
            FO.write(buf,0,count);
        }
        System.out.println("File" + FileName + " is created" );
        SI.close();
        FO.close();
    }

}
