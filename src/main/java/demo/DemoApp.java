package demo;

import bibernate.session.SessionFactory;
import demo.entity.Person;

public class DemoApp {
    public static void main(String[] args) {
        SessionFactory sessionFactory = null;
        var session = sessionFactory.openSession();

        var person = session.find(Person.class, 7L);
        System.out.println(person);
    }
}
