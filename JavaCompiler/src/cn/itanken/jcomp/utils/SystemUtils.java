package cn.itanken.jcomp.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

//import cn.itanken.jcomp.MainFrame;

public class SystemUtils {

	private static ImageIcon imageIcon = new ImageIcon(SystemUtils.class.getResource("../res/image/tbhan"));
	
    /**
     * 执行外部程序
     * @param cmdStr
     */
    public static void exeC(String[] cmdStr) {
        Runtime rn = Runtime.getRuntime();
        Process p = null;
        try { 
    		p = rn.exec(cmdStr); 
    		InputStream is = p.getErrorStream();
    		BufferedReader br = new BufferedReader(new InputStreamReader(is, "GBK"));
    		String error = "";
    		String line = br.readLine();
    		if (line != null && !line.isEmpty()) {
				error += line;
			}
    		if(!StrUtils.isEmpty(error)) {
    			//MainFrame.console.showError(error);
    		}
        } catch (Exception ex) {
        	JOptionPane.showMessageDialog (null, "命令执行失败："+ex, "错误", JOptionPane.PLAIN_MESSAGE, imageIcon);
        } finally {
        	if(p != null) {
        		p.destroy();
        		p = null;
        	}
        	rn = null;
        }
    }
}
