/*
 * 特别声明：本技术材料受《中华人民共和国著作权法》、《计算机软件保护条例》等法律、法规、行政
 * 规章以及有关国际条约的保护，赞同科技享有知识产权、保留一切权利并视其为技术秘密。未经本公司书
 * 面许可，任何人不得擅自（包括但不限于：以非法的方式复制、传播、展示、镜像、上载、下载）使用，
 * 不得向第三方泄露、透露、披露。否则，本公司将依法追究侵权者的法律责任。特此声明！
 *
 * Special Declaration: These technical material reserved as the technical secrets by AGREE 
 * TECHNOLOGY have been protected by the "Copyright Law" "ordinances on Protection of Computer 
 * Software" and other relevant administrative regulations and international treaties. Without 
 * the written permission of the Company, no person may use (including but not limited to the 
 * illegal copy, distribute, display, image, upload, and download) and disclose the above 
 * technical documents to any third party. Otherwise, any infringer shall afford the legal 
 * liability to the company.
 */
package cn.com.cpy.tools.gitlog.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * @author PuYun &lt;pu.yun@agree.com.cn&gt;
 * @version $Revision: 1.2 $ $Date: 2007/08/15 03:49:13 $
 */
public final class UTF8Properties extends Properties
{
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    private static final String WHITE_SPACE_CHARS = " \t\r\n\f";

    private static final String KEY_VALUE_SEPARATORS = "=: \t\r\n\f";

    private static final String STRICT_KEY_VALUE_SEPARATORS = "=:";

    private boolean continueLine(String line)
    {
        int slashCount = 0;
        int index = line.length() - 1;
        while ((index >= 0) && (line.charAt(index--) == '\\'))
            slashCount++;
        return (slashCount % 2 == 1);
    }

    @Override
    public synchronized void load(InputStream inStream) throws IOException
    {
        BufferedReader in = new BufferedReader(new InputStreamReader(inStream,
                "UTF-8"));
        while (true)
        {
            // Get next line
            String line = in.readLine();
            if (line == null)
                return;

            if (line.length() > 0)
            {
                // Find start of key
                int len = line.length();
                int keyStart;
                for (keyStart = 0; keyStart < len; keyStart++)
                    if (WHITE_SPACE_CHARS.indexOf(line.charAt(keyStart)) == -1)
                        break;

                // Blank lines are ignored
                if (keyStart == len)
                    continue;

                // Continue lines that end in slashes if they are not
                // comments
                char firstChar = line.charAt(keyStart);
                if ((firstChar != '#') && (firstChar != '!'))
                {
                    while (continueLine(line))
                    {
                        String nextLine = in.readLine();
                        if (nextLine == null)
                            nextLine = "";
                        String loppedLine = line.substring(0, len - 1);
                        // Advance beyond whitespace on new line
                        int startIndex;
                        for (startIndex = 0; startIndex < nextLine.length(); startIndex++)
                            if (WHITE_SPACE_CHARS.indexOf(nextLine
                                    .charAt(startIndex)) == -1)
                                break;
                        nextLine = nextLine.substring(startIndex,
                                nextLine.length());
                        line = loppedLine + nextLine;
                        len = line.length();
                    }

                    // Find separation between key and value
                    int separatorIndex;
                    for (separatorIndex = keyStart; separatorIndex < len; separatorIndex++)
                    {
                        char currentChar = line.charAt(separatorIndex);
                        if (currentChar == '\\')
                            separatorIndex++;
                        else if (KEY_VALUE_SEPARATORS.indexOf(currentChar) != -1)
                            break;
                    }

                    // Skip over whitespace after key if any
                    int valueIndex;
                    for (valueIndex = separatorIndex; valueIndex < len; valueIndex++)
                        if (WHITE_SPACE_CHARS.indexOf(line.charAt(valueIndex)) == -1)
                            break;

                    // Skip over one non whitespace key value separators
                    // if any
                    if (valueIndex < len
                            && STRICT_KEY_VALUE_SEPARATORS.indexOf(line
                                    .charAt(valueIndex)) != -1)
                        valueIndex++;

                    // Skip over white space after other separators if
                    // any
                    while (valueIndex < len)
                    {
                        if (WHITE_SPACE_CHARS.indexOf(line.charAt(valueIndex)) == -1)
                            break;
                        valueIndex++;
                    }
                    String key = line.substring(keyStart, separatorIndex);
                    String value = (separatorIndex < len) ? line.substring(
                            valueIndex, len) : "";

                    // Convert then store key and value
                    key = loadConvert(key);
                    value = loadConvert(value);
                    // 把最后#以后的部分去掉
                    int lastSharpIndex = value.lastIndexOf('#');
                    if (lastSharpIndex > 0)
                    {
                        value = value.substring(0, lastSharpIndex).trim();
                    }
                    put(key, value);
                }
            }
        }
    }

    private String loadConvert(String theString)
    {
        char aChar;
        int len = theString.length();
        StringBuilder outBuffer = new StringBuilder(len);

        for (int x = 0; x < len;)
        {
            aChar = theString.charAt(x++);
            if (aChar == '\\')
            {
                aChar = theString.charAt(x++);
                if (aChar == 'u')
                {
                    // Read the xxxx
                    int value = 0;
                    for (int i = 0; i < 4; i++)
                    {
                        aChar = theString.charAt(x++);
                        switch (aChar)
                        {
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                value = (value << 4) + aChar - '0';
                                break;
                            case 'a':
                            case 'b':
                            case 'c':
                            case 'd':
                            case 'e':
                            case 'f':
                                value = (value << 4) + 10 + aChar - 'a';
                                break;
                            case 'A':
                            case 'B':
                            case 'C':
                            case 'D':
                            case 'E':
                            case 'F':
                                value = (value << 4) + 10 + aChar - 'A';
                                break;
                            default:
                                throw new IllegalArgumentException(
                                        "Malformed \\uxxxx encoding.");
                        }
                    }
                    outBuffer.append((char) value);
                } else
                {
                    if (aChar == 't')
                        aChar = '\t';
                    else if (aChar == 'r')
                        aChar = '\r';
                    else if (aChar == 'n')
                        aChar = '\n';
                    else if (aChar == 'f')
                        aChar = '\f';
                    outBuffer.append(aChar);
                }
            } else
                outBuffer.append(aChar);
        }
        return outBuffer.toString();
    }
}
