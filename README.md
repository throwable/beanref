# BeanRef

A small library that allows you to access java bean properties in a static way using method reference lambdas. No code
generation or bytecode manipulation is required.

## The Idea

```java
Person person = ...
// create static path refrence
final BeanPath<Person, String> personCityProperty = 
    $(Person::getContact).$(Contact::getAddress).$(Address::getCity);
// the main goal is to obtain path in a static way
assertEquals("contact.address.city", personCityProperty.getPath());
// to access members transitively
personCityProperty.set(person, "Madrid");
// the same as person.getContact().getAddress().getCity()
assertEquals("Madrid", personCityProperty.get(person));
``` 

- static type-safe references to properties
- transitive access to nested properties
- get/set values via reference

### Why?

The main goal is to improve code maintainability and natively provide support of properties for your IDE: 
- compile-time control of property names
- code-completion
- simple analysis and refactoring of your code

### How?

We use a getter-method lambda to construct a reference to the property. The problem is that inside lambda we have no direct
information about target class and invoked method thus we need to use a simple trick: transform lambda to
`java.lang.invoke.SerializedLambda` object that allows us to obtain class name and method.
```java
public interface MethodReferenceLambda<BEAN, TYPE> extends Function<BEAN, TYPE>, Serializable {}
...
MethodReferenceLambda<Person,String> lambda = Person::getName(); 
Method writeMethod = lambda.getClass().getDeclaredMethod("writeReplace");
writeMethod.setAccessible(true);
SerializedLambda serLambda = (SerializedLambda) writeMethod.invoke(lambda);
String className = serLambda.getImplClass().replaceAll("/", ".");
String methodName = serLambda.getImplMethodName();
```
Resolving of a property takes a while thus we use cache to speedup successive resolutions. 

## Installation

### Maven
Available in JCenter repository
```xml
<repositories>
    <repository>
        <id>jcenter</id>
        <url>https://jcenter.bintray.com/</url>
    </repository>
</repositories>
...
<dependencies>
    ...
    <dependency>
        <groupId>com.github.throwable.beanref</groupId>
        <artifactId>beanref</artifactId>
        <version>0.1</version>
    </dependency>
</dependencies>
```
### Gradle

```groovy
repositories {
    mavenCentral()
    jcenter()
}
...
dependencies {
    ...
    compile 'com.github.throwable.beanref:beanref:0.1'
}
```

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
setter). If the name of getrer method does not start with "get/is") the whole getter's name is used as the name of a
property. In this case a correspondent setter method must also have it's name without "set" prefix. If no correspondent
setter method was found a property is declared as read-only.

#### Referencing nested properties:
```java
final BeanPath<Person, String> personCityProperty = 
    $(Person::getContact).$(Contact::getAddress).$(Address::getCity);
assertEquals("contact.address.city", personCityProperty.getPath());
assertEquals(Person.class, personCityProperty.getBeanClass());
assertEquals(String.class, personCityProperty.getType());
```

#### Accessing data
```java
Person person = ...
final BeanPath<Person, String> personCityProperty = 
    $(Person::getContact).$(Contact::getAddress).$(Address::getCity);
String personCity = personCityProperty.get(person);

person.setContact(null);
// this returns null while person.getContact().getAddress().getCity() throws NPE
assertNull(personCityProperty.get(person));
```
Accessing bean's property via path never throws NPE. Instead it returns null if the path is incomplete.

#### Mutating data
```java
Person person = ...
final BeanPath<Person, String> personCityProperty = 
    $(Person::getContact).$(Contact::getAddress).$(Address::getCity);
personCityProperty.set(person, "Madrid");
assertEquals("Madrid", personCityProperty.get(person));

person.setContact(null);
// this returns null while person.getContact().getAddress().getCity() throws NPE
assertNull(personCityProperty.get(person));
```
If path is incomplete the library tries to re-create missing transitive beans using no-args constructor (if
any). If it was not possible to instantiate missing bean an IncompletePathException is thrown.
```java
// empty object
Person person = new Person();
final BeanPath<Person, String> personCityProperty = 
    $(Person::getContact).$(Contact::getAddress).$(Address::getCity);
// no NPE, transitive Contact and Address are created automatically 
personCityProperty.set(person, "Madrid");
assertEquals("Madrid", personCityProperty.get(person));
assertEquals("Madrid", person.getContact().getAddress().getCity());
```

#### Collections support (bonus)

Sometimes it is needed to construct paths that reference elements inside a collection. 
In this case the library treats collections as a single-element containers.
Any access or mutation are made always over the last contained element.
```java
// dereferenced collection always works with the last element (if any)
final BeanPath<Person, String> personPhonePath = 
    $(Person::getContact).$$(Contact::getPhoneList).$(Phone::getPhone);
assertEquals("contact.phoneList.phone", personPhonePath.getPath());
assertEquals(personPhonePath.get(person), person.getContact().getPhoneList()
    .get(person.getContact().getPhoneList().size()-1).getPhone());
```

## License
[MIT](https://choosealicense.com/licenses/mit/)