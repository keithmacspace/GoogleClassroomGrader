package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Date;
import java.util.List;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.StudentData;

public class StudentPanel extends JPanel {
	private static final long serialVersionUID = 3480731067309159048L;
	private StudentListModel studentModel;
	private JTable studentTable;
	private StudentListRenderer studentListRenderer;
	private StudentListEditor studentListEditor;
	private StudentPanelListener studentPanelListener;	
	private VerticalTableHeaderCellRenderer verticalHeaderRenderer;
	private boolean resizing;
	private Rubric currentRubric;
	private ExcelAdapter excelAdapter;




	public StudentPanel() {
		setLayout(new BorderLayout());
		studentModel = new StudentListModel();
		studentTable = new JTable(studentModel) {
            //Implement table header tool tips. 
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        if (currentRubric != null) {
                        	realIndex -= StudentListModel.NUM_DEFAULT_COLUMNS;
                        	if (realIndex >= 0) {
                        		RubricEntry entry = currentRubric.getEntry(realIndex);
                        		if (entry != null) {
                        			return entry.getDescription();
                        		}
                        		
                        	}
                        }
                        return null;
                    }
                };
            }
            
		};		
		studentTable.setAutoCreateRowSorter(false);
		studentTable.setCellSelectionEnabled(true);
		studentTable.getTableHeader().setReorderingAllowed(false);				
		studentListRenderer = new StudentListRenderer();
		excelAdapter = new ExcelAdapter(studentTable);
		//studentListEditor = new StudentListEditor();
		
		verticalHeaderRenderer = new VerticalTableHeaderCellRenderer();
		studentTable.setDefaultRenderer(FileData.class, studentListRenderer);
		studentTable.setDefaultRenderer(CompilerMessage.class, studentListRenderer);
		studentTable.setDefaultRenderer(StudentData.class, studentListRenderer);
		studentTable.setDefaultEditor(String.class, studentListEditor);
		setHeaderRenderer();
	    addComponentListener( new ComponentListener() {
	        @Override
	        public void componentResized(ComponentEvent e) {
	            resizeColumns();
	        }

			@Override
			public void componentMoved(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void componentShown(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void componentHidden(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}
	    });
	    

//		studentTable.addMouseListener(new MouseAdapter() {			
//			@Override
//			public void mouseClicked(MouseEvent e) {				
//				super.mouseClicked(e);
//				if (studentPanelListener != null) {
//					if (e.getButton() == MouseEvent.BUTTON1) {
//						int row = studentTable.rowAtPoint(e.getPoint());
//						String id = studentModel.getStudentId(row);
//						if (id != null) {
//							studentPanelListener.studentSelected(id);
//						}
//					}
//					
//					
//				}
//			}
//			
//		});
		studentTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}
				ListSelectionModel lsm = (ListSelectionModel)e.getSource();
				if (lsm.isSelectionEmpty() == false) {
					int selectedRow = lsm.getMinSelectionIndex();
					String id = studentModel.getStudentId(selectedRow);
					if (id != null && studentPanelListener != null) {
						studentPanelListener.studentSelected(id);
					}
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
		resizing = true;
	    // Use TableColumnModel.getTotalColumnWidth() if your table is included in a JScrollPane
	    int width = studentTable.getWidth();
	    TableColumn column;

	    TableColumnModel jTableColumnModel = studentTable.getColumnModel();
	    int numCols = jTableColumnModel.getColumnCount();
	    final int FIXED_PREFERRED_SIZE = 30;
	    width = width - (FIXED_PREFERRED_SIZE * (numCols - StudentListModel.COMPILER_COLUMN));
	    width /= 3;
	    for (int i = 0; i < numCols; i++) {
	        column = jTableColumnModel.getColumn(i);
	        int preferredWidth = FIXED_PREFERRED_SIZE;
	        if (i < StudentListModel.COMPILER_COLUMN) {
	        	preferredWidth = width;
	        }
	        column.setPreferredWidth(preferredWidth);
	    }
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

	public void addStudent(StudentData student) {
		studentModel.addStudent(student);
		resizeColumns();
	}

	public void addColumn(String name) {
		studentModel.addColumn(name);
	}

	public void setAssignment(ClassroomData assignment) {
		studentListRenderer.setAssignment(assignment);
		studentModel.newAssignmentSelected();
		resizeColumns();
	}

	public void addCompilerMessages(List<CompilerMessage> messages) {
		studentModel.addCompilerMessages(messages);
	}

	public void addFileData(FileData fileDataInfo) {
		studentModel.addFileData(fileDataInfo);
	}

	public void setStudentPanelListener(StudentPanelListener studentPanelListener) {
		this.studentPanelListener = studentPanelListener;
	}
	
	public String[] getSelectedIds() {
		int [] selectedRows = studentTable.getSelectedRows();
		String [] ids = new String[selectedRows.length];
		for (int i = 0; i < selectedRows.length; i++) {
			ids[i] = studentModel.getStudentId(selectedRows[i]);
		}
		return ids;
	}
	
	public String getStudentId(int row) {
		return studentModel.getStudentId(row);
	}
	
	public void selectStudent(int row) {
		studentTable.setRowSelectionInterval(row, row);
	}
	
	public void setRubric(Rubric rubric) {
		currentRubric = rubric;
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				studentModel.setRubric(rubric);
				setHeaderRenderer();
				revalidate();
				resizeColumns();
			}
			
		});
	}
	 
	
	public List<List<Object> > getColumnValuesForSheet() {
		List<List<Object> > retVal = studentModel.getColumnValuesForSheet();
		int columnIndex = 0;
		for (int i = 0; i < retVal.size(); i++) {
			String columnName = studentTable.getColumnModel().getColumn(i).getHeaderValue().toString();
			columnName = columnName.replaceAll("<br>", "\n");
			columnName = columnName.replaceAll("<html>", "");
			columnName = columnName.replaceAll("</html>", "");
			retVal.get(i).add(0, columnName);
		}
		return retVal;
	}
	



}
