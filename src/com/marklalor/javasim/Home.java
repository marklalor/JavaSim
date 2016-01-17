package com.marklalor.javasim;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;

import com.marklalor.javasim.simulation.HomeMenu;
import com.marklalor.javasim.simulation.Simulation;
import com.marklalor.javasim.simulation.SimulationInfo;
import com.marklalor.javasim.simulation.frames.Minimizable;
import com.marklalor.javasim.text.Console;
import com.marklalor.javasim.text.JavaSimConsoleAppender;

public class Home extends JFrame implements ListSelectionListener, Minimizable
{
	private static final long serialVersionUID = 6321955323521519657L;
	
	private File homeDirectory;
	
	private List<SimulationInfo> simulations;
	private JList<SimulationInfo> simulationList;
	private JPanel simulationInfoPanel;
	
	private JLabel name, date, author, version, description;
	private JButton run;
	
	private HomeMenu menu;
	private Console console;
	
	public Home(File location, String[] args)
	{
		homeDirectory = location;
		if (args != null)
			parseCommandLineArgs(args);
		
		setUpConsole();
		loadSimulations();
		setUpLayout();
		
		
		menu = new HomeMenu(this);
		this.setJMenuBar(menu.getMenuBar());
	}
	
	private void setUpConsole()
	{
		console = new Console();
		
		if (JavaSim.CONSOLE_BIND)
		{
			JavaSimConsoleAppender consoleAppender = new JavaSimConsoleAppender(console);
			consoleAppender.setThreshold(Level.INFO);
			LogManager.getRootLogger().addAppender(consoleAppender);
		}
		
		JavaSim.getLogger().info("Console started.");
		JavaSim.getLogger().info("JavaSim version: {}", JavaSim.getVersion());	
	}

	private void parseCommandLineArgs(String args[])
	{
		CommandLineParser parser = new DefaultParser();
		
		Options options = new Options();
		options.addOption("n", "noconsolebind", false, "Do not bind the logger to the console.");
		
		try
		{
			CommandLine cmd = parser.parse(options, args);
			JavaSim.CONSOLE_BIND = !cmd.hasOption("noconsolebind");
		}
		catch(ParseException e)
		{
			JavaSim.getLogger().error("Could not parse the command line args properly.", e);
			return;
		}
	}

	private static final FilenameFilter jarFilter = new FilenameFilter()
	{
		@Override
		public boolean accept(File dir, String name) { return name.toLowerCase().endsWith("jar"); }
	};
	
	private void loadSimulations()
	{
		simulations = new ArrayList<SimulationInfo>();
		for (File jar : homeDirectory.listFiles(jarFilter))
		{
			SimulationInfo info = new SimulationInfo(jar);
			simulations.add(info);
		}
	}

	private void setUpLayout()
	{
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setTitle("Java Simulation Home");
		
		getContentPane().setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.BOTH;
		constraints.weighty = 1;
		
		DefaultListModel<SimulationInfo> model = new DefaultListModel<SimulationInfo>();
		if (simulations != null)
			for(SimulationInfo info : simulations)
				model.addElement(info);
		
		simulationList = new JList<SimulationInfo>(model);
		Font listFont = simulationList.getFont();
		simulationList.setFont(new Font(listFont.getName(), listFont.getStyle(), (int) (listFont.getSize()*1.50)));
		simulationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		simulationList.setLayoutOrientation(JList.VERTICAL);
		simulationList.setVisibleRowCount(-1);
		simulationList.addListSelectionListener(this);
		simulationList.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 4, new Color(0.25f, 0.10f, 0.10f)));
		
		JScrollPane simulationListScrollPane = new JScrollPane(simulationList);
		simulationListScrollPane.setPreferredSize(new Dimension(200, 100));
		simulationListScrollPane.setMinimumSize(new Dimension(200, 100));
		simulationListScrollPane.setBorder(BorderFactory.createEmptyBorder());
		getContentPane().add(simulationListScrollPane, constraints);
		
		simulationList.addMouseListener(new MouseAdapter()
		{
		    public void mouseClicked(MouseEvent evt)
		    {
		        if (evt.getClickCount() == 2)
		            runSelected();
		    }
		});
		
		simulationList.addKeyListener(new KeyListener()
		{
			
			@Override
			public void keyTyped(KeyEvent e) { }
			
			@Override
			public void keyReleased(KeyEvent e) { }
			
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
					runSelected();
			}
		});
		
		simulationInfoPanel = new JPanel();
		simulationInfoPanel.setLayout(new BoxLayout(simulationInfoPanel, BoxLayout.Y_AXIS));
		simulationInfoPanel.setBackground(new Color(0.90f, 0.85f, 0.85f));
		simulationInfoPanel.setBorder(BorderFactory.createMatteBorder(2, 6, 0, 0, new Color(0.90f, 0.85f, 0.85f)));
		constraints.weightx = 0.75;
		
		JScrollPane simulationInfoPanelScrollPane = new JScrollPane(simulationInfoPanel);
		simulationInfoPanelScrollPane.setBorder(BorderFactory.createEmptyBorder());
		getContentPane().add(simulationInfoPanelScrollPane, constraints);
		
		this.name = new JLabel();
		this.name.setFont(this.name.getFont().deriveFont(26f).deriveFont(Font.BOLD));
		this.date = new JLabel();
		this.author = new JLabel();
		this.version = new JLabel();
		this.description = new JLabel();
		simulationInfoPanel.add(this.name);
		simulationInfoPanel.add(this.date);
		simulationInfoPanel.add(this.author);
		simulationInfoPanel.add(this.version);
		simulationInfoPanel.add(this.description);
		
		this.run = new JButton("Run");
		this.run.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				runSelected();
			}
		});
		this.run.setVisible(false);
		simulationInfoPanel.add(this.run);
		
		simulationList.setSelectedIndex(0);
	}
	
	private void runSelected()
	{
		this.run((SimulationInfo) simulationList.getSelectedValue());
	}
	
	public void run(SimulationInfo info)
	{
		JavaSim.getLogger().info("Running {}", info.getName());
		
		Class<? extends Simulation> simClass = null;
		
		try
		{
			simClass = SimulationInfo.loadSimulationClass(info.getFile(), info.getMain()==null?null:(info.getMain()==""?null:info.getMain()));
		}
		catch(ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		if (simClass == null)
		{
			JavaSim.getLogger().error("Could not load the simulation class! {}", info);
			return;
		}
		
		try
		{
			JavaSim.getLogger().info("Initializing {}", simClass.getName());
			try
			{
				final Simulation simulation = simClass.newInstance();
				simulation.preInitialize(this, info);
				simulation.initialize();
				simulation.postInitialize();
				simulation.resetAction();
			}
			catch(IllegalArgumentException |  SecurityException e)//InvocationTargetException | NoSuchMethodException |
			{
				e.printStackTrace();
			}
		}
		catch(InstantiationException e)
		{
			e.printStackTrace();
		}
		catch(IllegalAccessException e)
		{
			e.printStackTrace();
		}
	}
	
	public Console getConsole()
	{
		return console;
	}
	
	public File getHomeDirectory()
	{
		return homeDirectory;
	}
	
	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		this.run.setVisible(true);
		SimulationInfo info = (SimulationInfo) simulationList.getSelectedValue();
		this.name.setText(info.getName());
		this.date.setText(info.getDate());
		this.author.setText(info.getAuthor());
		this.version.setText("Version " + info.getVersion());
		this.description.setText("<html><p>" + info.getDescription() + "</p></html>");
	}
	
	@Override
	public void minimize()
	{
		if (isVisible())
			setState(ICONIFIED);
	}
}
