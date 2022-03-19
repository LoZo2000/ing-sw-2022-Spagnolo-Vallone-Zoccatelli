package it.polimi.ingsw.model;

import it.polimi.ingsw.model.exceptions.TooManyStudentsException;

import java.util.ArrayList;

public class Cloud {
    private final int maxStudents;
    ArrayList<Student> students;

    public Cloud(int maxStudents){
        this.maxStudents = maxStudents;
        students = new ArrayList<Student>();
    }

    //To pass students one at a time
    public void updateStudents(Student student) throws TooManyStudentsException {
        if(students.size()<maxStudents) students.add(student);
        else throw new TooManyStudentsException();
    }

    //To pass all the students in a single call
    public void updateStudents(ArrayList<Student> students) throws TooManyStudentsException{
        if(students.size()==maxStudents)
            this.students = students;
        else throw new TooManyStudentsException();
    }

    //Returns a copy of students (to let the users examine the cloud)
    public ArrayList<Student> getStudents(){
        return (ArrayList<Student>)students.clone();
    }

    //Returns the students and empties the cloud
    public ArrayList<Student> chooseCloud(){
        ArrayList<Student> s = (ArrayList<Student>)students.clone();
        students.clear();
        return s;
    }
}