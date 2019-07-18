package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.DefaultEditorKit;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.listenerCoordinator.StudentListInfo;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.StudentData;

public class StudentPanel extends JPanel {
	private static final long serialVersionUID = 3480731067309159048L;
	private StudentListModel studentModel;
	private JTable studentTable;
	private StudentListRenderer studentListRenderer;
	private StudentPanelListener studentPanelListener;	
	private VerticalTableHeaderCellRenderer verticalHeaderRenderer;
	private boolean resizing;
	private JPopupMenu rightClickPopup;

	public StudentPanel(StudentListInfo studentListInfo) {
		setLayout(new BorderLayout());
		studentModel = new StudentListModel(studentListInfo);
		studentTable = new JTable(studentModel) {
			private static final long serialVersionUID = 1L;
			//Implement table header tool tips. 
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
					private static final long serialVersionUID = 1L;
					public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        return studentListInfo.getColumnTip(index);
                    }
                };
            }
            
		};		
		studentTable.setAutoCreateRowSorter(false);
		studentTable.setCellSelectionEnabled(true);
		studentTable.getTableHeader().setReorderingAllowed(false);				
		studentListRenderer = new StudentListRenderer();
		new ExcelAdapter(studentTable);

		
		verticalHeaderRenderer = new VerticalTableHeaderCellRenderer();
		studentTable.setDefaultRenderer(FileData.class, studentListRenderer);
		studentTable.setDefaultRenderer(CompilerMessage.class, studentListRenderer);
		studentTable.setDefaultRenderer(StudentData.class, studentListRenderer);
		
		createPopupMenu();

		setHeaderRenderer();
	    addComponentListener( new ComponentListener() {
	        @Override
	        public void componentResized(ComponentEvent e) {
	        	SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {					   
						resizeColumns();						
					}
	        		
	        	});	            
	        }
			@Override
			public void componentMoved(ComponentEvent e) {				
			}

			@Override
			public void componentShown(ComponentEvent e) {				
			}

			@Override
			public void componentHidden(ComponentEvent e) {
			}
	    });
	    

		studentTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}
				ListSelectionModel lsm = (ListSelectionModel)e.getSource();
				if (lsm.isSelectionEmpty() == false) {
					int selectedRow = lsm.getMinSelectionIndex();
					Object student = studentModel.getValueAt(selectedRow, StudentListInfo.LAST_NAME_COLUMN);
					if (student != null && studentPanelListener != null) {
						studentPanelListener.studentSelected(((StudentData)student).getId());
					}
				}
				
				
			}
			
		});
		// studentList.setRowHeight(25);
		add(new JScrollPane(studentTable), BorderLayout.CENTER);
	}
	
	private void createPopupMenu() {
		rightClickPopup = new JPopupMenu();
		Action copy = new DefaultEditorKit.CopyAction();
		rightClickPopup.add(copy);
		Action paste = new DefaultEditorKit.PasteAction();
		rightClickPopup.add(paste);
		
		JMenuItem addRubricColumn = new JMenuItem("Add Rubric Column...");
		rightClickPopup.add(addRubricColumn);
		addRubricColumn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (studentPanelListener != null) {
					studentPanelListener.openRubricEditorDialog();					
				}
			}
			
		});
		
		studentTable.setComponentPopupMenu(rightClickPopup);
	}
	


	private void resizeColumns() {
		if (resizing) {
			return;
		}
		resizing = true;
	    // Use TableColumnModel.getTotalColumnWidth() if your table is included in a JScrollPane
	    int width = studentTable.getWidth();
	    TableColumn column;

	    TableColumnModel jTableColumnModel = studentTable.getColumnModel();	    
	    int numCols = jTableColumnModel.getColumnCount();
	    final int FIXED_PREFERRED_SIZE = 30;
	    width = width - (FIXED_PREFERRED_SIZE * (numCols - StudentListInfo.COMPILER_COLUMN));
	    width /= 3;
	    for (int i = 0; i < numCols; i++) {
	        column = jTableColumnModel.getColumn(i);
	        int preferredWidth = FIXED_PREFERRED_SIZE;
	        if (i < StudentListInfo.COMPILER_COLUMN) {
	        	preferredWidth = width;
	        }
	        column.setPreferredWidth(preferredWidth);
	    }
	    setHeaderRenderer();
	    resizing = false;
	}
	
	private void setHeaderRenderer() {
		TableColumnModel columnModel = studentTable.getColumnModel();
		for (int i = 0; i < columnModel.getColumnCount(); i++) {
			columnModel.getColumn(i).setHeaderRenderer(verticalHeaderRenderer);
		}
		
	}

	public void clearStudents() {
		studentModel.clearAll();
	}

	public void assignmentSet() {
		resizeColumns();
	}

	public void setStudentPanelListener(StudentPanelListener studentPanelListener) {
		this.studentPanelListener = studentPanelListener;
	}
	
	public List<String> getSelectedIds() {
		int [] selectedRows = studentTable.getSelectedRows();
		List<String> ids = new ArrayList<String>();		
		for (int i = 0; i < selectedRows.length; i++) {
			Object student = studentModel.getValueAt(selectedRows[i], StudentListInfo.LAST_NAME_COLUMN);
			if (student != null) {
				ids.add(((StudentData)student).getId()); 
			}
		}
		return ids;
	}
	
	public void selectStudent(int row) {
		studentTable.setRowSelectionInterval(row, row);
	}
	
	public void setRubric(Rubric rubric) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				setHeaderRenderer();
				revalidate();
				resizeColumns();
			}
			
		});
	}
	
	public void dataChanged() {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				studentModel.fireTableDataChanged();				
			}
			
		});		
		
	}
	
	public void structureChanged() {
		studentModel.structureChanged();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				setHeaderRenderer();
				revalidate();
				resizeColumns();
			}
		});
		
	}
	

}
