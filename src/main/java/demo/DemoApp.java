package demo;

import bibernate.session.SessionFactory;
import bibernate.session.impl.SessionFactoryImpl;
import demo.entity.Note;
import demo.entity.Person;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;

public class DemoApp {
    public static void main(String[] args) {
        SessionFactory sessionFactory = new SessionFactoryImpl(initializeDataSource());
        var session = sessionFactory.openSession();

        var person = session.find(Person.class, 7L);
        System.out.println(person);
        var theSamePerson = session.find(Person.class, 7L);
        System.out.println(theSamePerson);
        System.out.println(person == theSamePerson);
        session.close();


        var anotherPerson = session.find(Person.class, 6L);
        System.out.println(anotherPerson);
        
        
    }

    private static DataSource initializeDataSource() {
        var dataSource = new PGSimpleDataSource();
        dataSource.setUrl("jdbc:postgresql://localhost:5432/postgres");
        dataSource.setUser("postgres");
        dataSource.setPassword("postgres");
        return dataSource;
    }
}
