package com.github.throwable.beanutil;

import com.github.throwable.beanutil.beans.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.throwable.beanutil.BeanUtil.$;
import static org.junit.Assert.*;

public class BeanUtilTest {
    @Test
    public void testBeanProperty() {
        final Person person = buildSamplePerson();

        final BeanProperty<Person, String> personNameProperty = $(Person::getName);
        assertEquals("name", personNameProperty.getPath());
        assertEquals(Person.class, personNameProperty.getBeanClass());
        assertEquals(String.class, personNameProperty.getType());
        assertTrue(personNameProperty.isReadOnly());
        assertEquals("Antón", personNameProperty.get(person));

        final BeanProperty<Person, Integer> personAgeProperty = $(Person::getAge);
        assertEquals("age", personAgeProperty.getPath());
        assertEquals(Integer.TYPE, personAgeProperty.getType());
        assertEquals(Integer.valueOf(22), personAgeProperty.get(person));
        assertFalse(personAgeProperty.isReadOnly());
        personAgeProperty.set(person, 30);
        assertEquals(30, person.getAge());
    }


    @Test(expected = ReadOnlyPropertyException.class)
    public void testReadOnly() {
        final Person person = buildSamplePerson();

        final BeanProperty<Person, String> personNameProperty = $(Person::getName);
        personNameProperty.set(person, "Peter");
    }


    @Test
    public void testBeanPath() {
        final Person person = buildSamplePerson();

        //final BeanPath<Person, Phone, String> personPhone = $(Person::getContact).$(Contact::getPhoneList, 0).$(Phone::getPhone);
        final BeanPath<Person, Address, String> personCity = $(Person::getContact).$(Contact::getAddress).$(Address::getCity);
        assertEquals("contact.address.city", personCity.getPath());
        assertEquals(Person.class, personCity.getBeanClass());
        assertEquals(String.class, personCity.getType());
        //assertEquals(Address.class, personCity.getBeanProperty().getBeanClass());
        assertTrue(personCity.isAccessible(person));
        assertEquals("Noville", personCity.get(person));

        final BeanPath<Person, Status, String> personStatusName = $(Person::getStatus).$(AtomicReference::get).$(Status::getName);
        // non-canonical getter method name
        assertEquals("status.get.name", personStatusName.getPath());
        assertEquals("married", personStatusName.get(person));
        assertTrue(personStatusName.isAccessible(person));

        // setter
        personCity.set(person, "Smallville");
        assertEquals("Smallville", person.getContact().getAddress().getCity());

        // make inaccessible
        person.getStatus().set(null);
        assertFalse(personStatusName.isAccessible(person));
        assertTrue($(Person::getStatus).$(AtomicReference::get).isAccessible(person));
        assertNull(personStatusName.get(person));   // return null instead of NPE

        try {
            personStatusName.set(person, "divorced");
            fail("Inaccessible path must throw InaccessiblePathException");
        } catch (InaccessiblePathException e) {/*ignore*/}
    }


    private Person buildSamplePerson() {
        final Person person = new Person("123456", "Antón");
        person.setAge(22);

        final Contact contact = new Contact();
        contact.setEmail("abc@mycompany.com");

        final Address address = new Address();
        address.setAddress("Flower street, 1-A");
        address.setCity("Noville");
        address.setState("Goodland");
        address.setZipCode(12345);
        contact.setAddress(address);
        List<Phone> phoneList = new ArrayList<>();
        final Phone phone1 = new Phone();
        phone1.setPhone("555-123-567");
        phone1.setType(Phone.Type.home);
        phoneList.add(phone1);
        final Phone phone2 = new Phone();
        phone2.setPhone("555-000-001");
        phone2.setType(Phone.Type.mobile);
        phoneList.add(phone2);
        contact.setPhoneList(phoneList);
        person.setContact(contact);

        final Status status = new Status();
        status.setName("married");
        status.setDescription("with children");
        person.getStatus().set(status);
        return person;
    }
}
