package org.nyu.edu.dlts.utils;

/**
 * Created by IntelliJ IDEA.
 * User: nathan
 * Date: 10/28/13
 * Time: 9:54 AM
 *
 * Simple interface used so that both a servlet and desktop client can be used to
 * print out information to the user
 */
public interface PrintConsole {
    /**
     * Pass the message to be printed
     * @param message
     */
    void print(String message);
}
