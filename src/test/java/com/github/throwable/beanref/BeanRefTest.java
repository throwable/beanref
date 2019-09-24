package com.github.throwable.beanref;

import com.github.throwable.beanref.beans.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.throwable.beanref.BeanRef.$;
import static com.github.throwable.beanref.BeanRef.$$;
import static org.junit.Assert.*;

public class BeanRefTest {
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


    @Test(expected = IllegalArgumentException.class)
    public void testIllegalReference() {
        // Not implemented yet
        $((Person o) -> o.getContact().getEmail());
    }


    @Test
    public void testBeanPath() {
        final Person person = buildSamplePerson();

        //final BeanPath<Person, Phone, String> personPhone = $(Person::getContact).$(Contact::getPhoneList, 0).$(Phone::getPhone);
        final BeanPath<Person, String> personCity = $(Person::getContact).$(Contact::getAddress).$(Address::getCity);
        assertEquals("contact.address.city", personCity.getPath());
        assertEquals(Person.class, personCity.getBeanClass());
        assertEquals(String.class, personCity.getType());
        //assertEquals(Address.class, personCity.getBeanProperty().getBeanClass());
        assertTrue(personCity.isComplete(person));
        assertEquals("Noville", personCity.get(person));

        final BeanPath<Person, String> personStatusName = $(Person::getStatus).$(AtomicReference::get).$(Status::getName);
        // non-canonical getter method name
        assertEquals("status.get.name", personStatusName.getPath());
        assertEquals("married", personStatusName.get(person));
        assertTrue(personStatusName.isComplete(person));

        // setter
        personCity.set(person, "Smallville");
        assertEquals("Smallville", person.getContact().getAddress().getCity());

        // make incomplete
        person.getStatus().set(null);
        assertFalse(personStatusName.isComplete(person));
        assertTrue($(Person::getStatus).$(AtomicReference::get).isComplete(person));
        assertNull(personStatusName.get(person));   // return null instead of NPE

        try {
            personStatusName.set(person, "divorced");
            fail("Inaccessible path must throw InaccessiblePathException");
        } catch (IncompletePathException e) {/*ignore*/}
    }


    @Test
    public void testCollectionAccess() {
        final Person person = buildSamplePerson();

        final BeanPath<Person, String> personPhone = $(Person::getContact).$$(Contact::getPhoneList).$(Phone::getPhone);
        final BeanPath<Person, Phone> personPhoneList = $(Person::getContact).$$(Contact::getPhoneList);
        assertEquals("contact.phoneList.phone", personPhone.getPath());

        // always get last element
        assertEquals("555-000-001", personPhone.get(person));

        // set last element value
        personPhone.set(person, "123456789");
        assertEquals(2, person.getContact().getPhoneList().size());
        assertEquals("123456789", person.getContact().getPhoneList().get(1).getPhone());

        // remove all elements
        personPhoneList.set(person, null);
        assertTrue(person.getContact().getPhoneList().isEmpty());

        // implicitly create new element
        personPhone.set(person, "555-111-000");
        assertEquals(1, person.getContact().getPhoneList().size());
        assertEquals("555-111-000", person.getContact().getPhoneList().get(0).getPhone());

        // implicitly create new list
        person.getContact().setPhoneList(null);
        assertNull(personPhone.get(person));    // null instead of NPE
        assertFalse(personPhone.isComplete(person));
        personPhone.set(person, "12345");
        assertEquals(1, person.getContact().getPhoneList().size());
        assertEquals("12345", person.getContact().getPhoneList().get(0).getPhone());

        // Set usage
        $$(Person::getPermissions, HashSet::new);

        final BeanProperty<Person, String> personPermissions = $$(Person::getPermissions);
        personPermissions.set(person, "root");
        assertEquals(1, person.getPermissions().size());
        assertTrue(person.getPermissions().contains("root"));
        assertEquals("root", personPermissions.get(person));

        person.setPermissions(null);
        // wrong collection supplier
        try {
            $$(Person::getPermissions, ArrayList::new).set(person, "12345");
            fail("Collection must not be created");
        } catch (IncompletePathException e) {/*ignore*/}
        // right collection supplier
        $$(Person::getPermissions, HashSet::new).set(person, "12345");
        assertEquals(1, person.getPermissions().size());
        assertTrue(person.getPermissions().contains("12345"));
        assertEquals("12345", personPermissions.get(person));
    }


    @Test
    public void testBeanPropertyCache() {
        // same reference must be cached
        BeanProperty<Person, String> p0 = null;
        for (int i = 0; i < 10; i++) {
            final BeanProperty<Person, String> p = $(Person::getName);
            if (p0 == null) p0 = p;
            else assertSame(p0, p);
        }
    }

    // This wildcard actually does not work as desired
    // No difference between TCOL and T: both are bounded to Collection<TYPE>.
    /*public static <BEAN, TYPE, TCOL extends Collection<TYPE>, T extends TCOL> void $t(
            MethodReferenceLambda<BEAN, TCOL> methodReferenceLambda, Supplier<T> supplier) {
        TCOL col = supplier.get();
    }*/


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
