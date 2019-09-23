# BeanRef

A small library that allows you to access java bean properties in a static way using method reference lambdas. No code
generation or bytecode manipulation is required.

## The Idea

We want to be hable to do something like this:

```java
Person person = ...
// create static path refrence
final BeanPath<Person, String> personCityProperty = $(Person::getContact).$(Contact::getAddress).$(Address::getCity);
// the main goal is to hable to obtain path in a static way
assertEquals("contact.address.city", personCityProperty.getPath());
// hable to access members transitively
personCityProperty.set(person, "Madrid");
// the same as person.getContact().getAddress().getCity()
assertEquals("Madrid", personCityProperty.get(person));
``` 

- static type-safe reference to all properties
- transitive access to nested properties
- get/set values via reference

### Why?

Currently and in the nearest future Java will not have any physical facilities to work with object's properties and the
only way to reference java bean's properties is to use their names as string values. The advantages of static references
are:
- compile-time control of property names
- IDE support for code-completion
- easy to maintain, analyze and refactor

### How?

We pass a getter-method lambda to construct a reference to a property. The problem is that inside lambda we have no
direct reference to target class and invoked method thus we use a trick to transform lambda to
`java.lang.invoke.SerializedLambda` object that allows us to obtain class name and method.

## Installation

TODO: ...

## Usage

#### Obtain reference to an object's property via getter method
```java


final BeanProperty<Person, String> personNameProperty = $(Person::getName);
assertEquals("name", personNameProperty.getPath());
assertEquals(Person.class, personNameProperty.getBeanClass());
assertEquals(String.class, personNameProperty.getType());
// true because there is no correspondent setter method
assertTrue(personNameProperty.isReadOnly());
```
Getter/setter method names may follow the convention of java beans (getXXX()/isXXX() for getter and setXXX(xxx) for
setter). If a getter method's name does not start with "get/is) the whole getter name is used as a name of a property.
In this case a correspondent setter method must also have it's name without "set" prefix. If no correspondent setter
method was found a property is declared as read-only.

#### Referencing nested properties:
```java
final BeanPath<Person, String> personCityProperty = $(Person::getContact).$(Contact::getAddress).$(Address::getCity);
assertEquals("contact.address.city", personCityProperty.getPath());
assertEquals(Person.class, personCityProperty.getBeanClass());
assertEquals(String.class, personCityProperty.getType());
```

#### Accessing data
```java
Person person = ...
final BeanPath<Person, String> personCityProperty = $(Person::getContact).$(Contact::getAddress).$(Address::getCity);
String personCity = personCityProperty.get(person);

person.setContact(null);
// this returns null while person.getContact().getAddress().getCity() throws NPE
assertNull(personCityProperty.get(person));
```
Accessing bean's property via path never throws NPE. Instead if the path is not complete it returns null.

#### Mutating data
```java
Person person = ...
final BeanPath<Person, String> personCityProperty = $(Person::getContact).$(Contact::getAddress).$(Address::getCity);
personCityProperty.set(person, "Madrid");
assertEquals("Madrid", personCityProperty.get(person));

person.setContact(null);
// this returns null while person.getContact().getAddress().getCity() throws NPE
assertNull(personCityProperty.get(person));
```
If the path is not complete the library tries to re-create missing transitive beans in the path using bean's no-args
constructor (if any). If it is not possible to instantiate any bean an IncompletePathException is thrown.
```java
// empty object
Person person = new Person();
final BeanPath<Person, String> personCityProperty = $(Person::getContact).$(Contact::getAddress).$(Address::getCity);
// no NPE, transitive Contact, Address are created automatically 
personCityProperty.set(person, "Madrid");
assertEquals("Madrid", personCityProperty.get(person));
assertEquals("Madrid", person.getContact().getAddress().getCity());
```

#### Collections support (bonus)

Sometimes it is necessary to treat collections as a single-element containers and access nested elements directly.
```java
// dereferenced collection always works with the last element (if any)
final BeanPath<Person, String> personPhonePath = $(Person::getContact).$$(Contact::getPhoneList).$(Phone::getPhone);
assertEquals(personPhonePath.get(person), person.getContact().getPhoneList()
    .get(person.getContact().getPhoneList().size()-1).getPhone());
```


## License
[MIT](https://choosealicense.com/licenses/mit/)