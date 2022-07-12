package demo;

import bibernate.session.SessionFactory;
import bibernate.session.impl.SimpleSessionFactory;
import demo.entity.Person;
import lombok.extern.log4j.Log4j2;
import org.h2.jdbcx.JdbcDataSource;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;

@Log4j2
public class DemoApp {
    public static void main(String[] args) {
        SessionFactory sessionFactory = new SimpleSessionFactory(initializeInMemoryH2DataSource());
        var session = sessionFactory.openSession();

        System.out.println("👉 Loading a person by id = 1");
        var person = session.find(Person.class, 1L);
        System.out.println("👉 Loaded a person: " + person);

        var newLastName = "Super " + person.getLastName();
        System.out.println("👉 Changing a person's last name: " + person.getLastName() + " -> " + newLastName);
        person.setLastName(newLastName);

        System.out.println("👉 Printing Notes");
        person.getNotes().forEach(note -> System.out.println(" - " + note.getBody()));

        System.out.println("👉 Loading the same person by id = 1");
        var theSamePerson = session.find(Person.class, 1L);
        System.out.println("👉 Loaded a person: " + theSamePerson);
        System.out.println("👉 (person == theSamePerson) " + (person == theSamePerson));


        System.out.println("👉 Loading another person by id = 5");
        var anotherPerson = session.find(Person.class, 5L);
        System.out.println("👉 Removing a person: " + anotherPerson);
        session.remove(anotherPerson);

        var newPerson = createNewPerson();
        System.out.println("👉 Persisting a new person: " + newPerson);
        session.persist(newPerson);

        session.close();
    }

    private static Person createNewPerson() {
        var person = new Person();
        person.setId(6L);
        person.setFirstName("Josh");
        person.setLastName("Long");
        return person;
    }

    private static DataSource initializeInMemoryH2DataSource() {
        var dataSource = new JdbcDataSource();
        dataSource.setUrl("jdbc:h2:mem:demo;INIT=runscript from 'classpath:db/init.sql'");
        dataSource.setUser("sa");
        return dataSource;
    }

    private static DataSource initializePostgresDataSource() {
        var dataSource = new PGSimpleDataSource();
        dataSource.setUrl("jdbc:postgresql://localhost:5432/postgres");
        dataSource.setUser("postgres");
        dataSource.setPassword("postgres");
        return dataSource;
    }
}
