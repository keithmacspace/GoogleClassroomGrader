package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import net.cdonald.googleClassroom.googleClassroomInterface.SaveGrades;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.SetInfoLabelListener;
import net.cdonald.googleClassroom.listenerCoordinator.StudentListInfo;
import net.cdonald.googleClassroom.listenerCoordinator.StudentSelectedListener;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.StudentData;

public class StudentPanel extends JPanel {
	private static final long serialVersionUID = 3480731067309159048L;
	private StudentListModel studentModel;
	private JTable studentTable;
	private StudentListRenderer studentListRenderer;	
	private VerticalTableHeaderCellRenderer verticalHeaderRenderer;
	private volatile boolean resizing;
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
					String studentId = null;
					if (student != null) {
						studentId = ((StudentData)student).getId();
					}
					ListenerCoordinator.fire(StudentSelectedListener.class, studentId);
				}				
			}
		});
		
		studentTable.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
			@Override
			public void columnAdded(TableColumnModelEvent e) {
			}

			@Override
			public void columnRemoved(TableColumnModelEvent e) {
			}

			@Override
			public void columnMoved(TableColumnModelEvent e) {
			}

			@Override
			public void columnMarginChanged(ChangeEvent e) {
			}

			@Override
			public void columnSelectionChanged(ListSelectionEvent e) {
				
				ListSelectionModel lsm = (ListSelectionModel)e.getSource();
				if (lsm.isSelectionEmpty() == false) {
					int selectedColumn = lsm.getMaxSelectionIndex();					
					ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUBRIC_INFO, studentListInfo.getColumnTip(selectedColumn));
				}
			}			
		});
		// studentList.setRowHeight(25);
		add(new JScrollPane(studentTable), BorderLayout.CENTER);
	}
	



	private void resizeColumns() {
		if (resizing) {
			return;
		}

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
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
			});
		
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
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				studentModel.structureChanged();
				setHeaderRenderer();
				revalidate();
				resizeColumns();
			}
		});		
	}
	
	public void addStudentGrades(SaveGrades saveGrades, Rubric rubric) {

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				for (int i = 0; i < studentModel.getRowCount(); i++) {
					if (studentTable.getCellEditor() != null) {
						studentTable.getCellEditor().stopCellEditing();
					}
					StudentData studentInfo = (StudentData)studentModel.getValueAt(i, StudentListInfo.LAST_NAME_COLUMN);
					saveGrades.addStudentColumn(studentInfo, StudentListInfo.defaultColumnNames[StudentListInfo.LAST_NAME_COLUMN], studentInfo.getName());
					saveGrades.addStudentColumn(studentInfo, StudentListInfo.defaultColumnNames[StudentListInfo.FIRST_NAME_COLUMN], studentInfo.getFirstName());
					String date = (String)studentModel.getValueAt(i, StudentListInfo.DATE_COLUMN);
					saveGrades.addStudentColumn(studentInfo, StudentListInfo.defaultColumnNames[StudentListInfo.DATE_COLUMN], date);
					for (RubricEntry entry : rubric.getEntries()) {
						Double grade = entry.getStudentDoubleValue(studentInfo.getId());
						if (grade != null) {
							saveGrades.addStudentColumn(studentInfo, entry.getName(), grade);
						}
					}
				}
			}
		});

	}

}
