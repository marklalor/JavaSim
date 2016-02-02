package com.marklalor.javasim.menu;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import com.marklalor.javasim.JavaSim;
import com.marklalor.javasim.simulation.frames.FrameHolder;

public abstract class Menu implements ActionListener
{
    private FrameHolder frameHolder;
    private MenuHandler menuHandler;
    private JMenuBar menuBar;
    
    public void createMenu(FrameHolder frameHolder, MenuHandler menuHandler)
    {
        this.frameHolder = frameHolder;
        this.menuHandler = menuHandler;
        this.menuHandler.setMenu(this);
        
        this.menuBar = new JMenuBar();
        
        createMenuItems();
        finishInitializingMenuItems();
        createMenuHeaders();
        finishInitializingMenuHeaders();
    }
    
    private void createMenuItems()
    {
        // Reflection ain't the prettiest behind the scenes.
        // Parsing the menu names:
        try
        {
            for(Field field : getClass().getDeclaredFields())
            {
                if(field.getType().equals(JMenuItem.class) && field.isAnnotationPresent(MenuItem.class))
                {
                    field.setAccessible(true);
                    
                    // Get all the info about this field from the name/annotations.
                    final String fieldName = field.getName();
                    final String customText = field.getAnnotation(MenuItem.class).text();
                    final int keyCode = field.getAnnotation(MenuItem.class).keyCode();
                    final int[] keyModifiers = field.getAnnotation(MenuItem.class).keyModifiers();
                    
                    // JMenuItem text
                    final String text;
                    
                    // If there annotation is there, either auto-resolve or
                    if(customText.equals(MenuItem.AUTO_RESOLVE_TEXT))
                        text = getMenuNameFromFieldName(fieldName);
                    else
                        text = customText;
                    
                    // Create the menu item, with the specified text / accelerator / action listener.
                    JMenuItem menuItem = new JMenuItem(text);
                    
                    // Set this class's actionPerformed method to handle the menu action.
                    menuItem.addActionListener(this);
                    
                    // Find the appropriate method from the given MenuHandler to deal with the action.
                    Method methodWithSameNameAsMenuItem = menuHandler.getClass().getDeclaredMethod(fieldName, new Class[] {});
                    menuHandler.mapMenuItemToMethod(menuItem, methodWithSameNameAsMenuItem);
                    
                    if(keyCode != MenuItem.NO_ACCELERATOR_KEYCODE)
                    {
                        // Create the key modifier from all the bitmasks given.
                        int bitCombinedKeyModifier = 0;
                        for(int modifier : keyModifiers)
                        {
                            if(modifier == -1)
                                bitCombinedKeyModifier = bitCombinedKeyModifier | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
                            else
                                bitCombinedKeyModifier = bitCombinedKeyModifier | modifier;
                        }
                        
                        menuItem.setAccelerator(KeyStroke.getKeyStroke(keyCode, bitCombinedKeyModifier));
                        JavaSim.getLogger().trace("Set accelerator for {}. Key Code: {}, Key Modifier: {}", text, keyCode, Integer.toBinaryString(bitCombinedKeyModifier));
                    }
                    field.set(this, menuItem);
                }
            }
        }
        catch(IllegalAccessException | SecurityException | IllegalArgumentException | NoSuchMethodException e)
        {
            JavaSim.getLogger().error("Error while resolving JMenuItem names. {}", e);
        }
    }
    
    private void createMenuHeaders()
    {
        try
        {
            for(Field field : getClass().getDeclaredFields())
            {
                if(field.getType().equals(JMenu.class) && field.isAnnotationPresent(MenuHeader.class))
                {
                    field.setAccessible(true);
                    
                    final String fieldName = field.getName();
                    final String customText = field.getAnnotation(MenuHeader.class).text();
                    final String[] children = field.getAnnotation(MenuHeader.class).children();
                    
                    final String text;
                    
                    // If there annotation is there, either auto-resolve or
                    if(customText.equals(MenuHeader.AUTO_RESOLVE_TEXT))
                        text = getMenuNameFromFieldName(fieldName);
                    else
                        text = customText;
                    
                    JMenu menu = new JMenu(text);
                    
                    for(String child : children)
                    {
                        if(!child.equals(MenuHeader.SEPERATOR))
                        {
                            JavaSim.getLogger().debug("Getting field {} from class {}.", child, this.getClass().getCanonicalName());
                            Field menuItemField = this.getClass().getDeclaredField(child);
                            menuItemField.setAccessible(true);
                            JMenuItem item = (JMenuItem) menuItemField.get(this);
                            menu.add(item);
                        }
                        else
                            menu.addSeparator();
                    }
                    
                    menuBar.add(menu);
                }
            }
        }
        catch(IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e)
        {
            JavaSim.getLogger().error("Error while resolving setting up JMenu. {}", e);
        }
    }
    
    protected abstract void finishInitializingMenuItems();
    
    protected abstract void finishInitializingMenuHeaders();
    
    private static String getMenuNameFromFieldName(String fieldName)
    {
        String splitCamelCase = fieldName.replaceAll(String.format("%s|%s|%s",
                "(?<=[A-Z])(?=[A-Z][a-z])",
                "(?<=[^A-Z])(?=[A-Z])",
                "(?<=[A-Za-z])(?=[^A-Za-z])"
                ), " ");
        return splitCamelCase.substring(0, 1).toUpperCase() + splitCamelCase.substring(1);
    }
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        JavaSim.getLogger().info("{}\u2192{}", ((JMenu) ((JPopupMenu) ((JMenuItem) e.getSource()).getParent()).getInvoker()).getText(), ((JMenuItem) e.getSource()).getText());
        menuHandler.action(e);
    }
    
    public FrameHolder getFrameHolder()
    {
        return frameHolder;
    }
    
    public JMenuBar getMenuBar()
    {
        if (menuBar == null)
            throw new MenuAccessException("The menu bar has not yet been created. Call Menu#createMenu() at some point before this, such as in the subclass' constructor.");
        return menuBar;
    }
    
    public MenuHandler getMenuHandler()
    {
        return menuHandler;
    }
}
