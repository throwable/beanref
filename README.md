# BeanRef

A small library that allows you to access java bean properties in a static way using method reference lambdas. No code
generation or bytecode manipulation is required.

## The Idea

```java
Person person = ...

// We can define property paths staticaly in a type-safe manner...
final BeanPath<Person, String> personCityProperty = 
    $(Person::getContact).$(Contact::getAddress).$(Address::getCity);
assertEquals("contact.address.city", personCityProperty.getPath());

// ...and also access property's value via BeanPath.
personCityProperty.set(person, "Madrid");

// When accessing transitive properties we do not throw NPEs.
// The same as person.getContact().getAddress().getCity() but without NPE.
assertEquals("Madrid", personCityProperty.get(person));
``` 

- To be able to define a static type-safe references to bean's properties or to transitive paths.
- Evaluate and access property's value via reference.
- Minimize NPE impact over property operations.

### Why?

The main goal is to improve code's maintainability and provide native support of properties for your IDE: 
- compile-time control of property names
- code-completion
- simple analysis and refactoring of your code

### How?

We use a getter-method lambda to construct a reference to the property. To obtain the information about target class
and the invoked method we transform out lambda into `java.lang.invoke.SerializedLambda` object that has all the necessary data.
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
Resolved properties' lambdas then cached to speedup successive resolutions. 

## Installation

### Maven

```xml
<dependencies>
    <dependency>
        <groupId>com.github.throwable.beanref</groupId>
        <artifactId>beanref</artifactId>
        <version>0.1</version>
    </dependency>
</dependencies>
```
### Gradle

```groovy
dependencies {
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
setter), but fluent accessor notation (xxx()) is also supported.

#### Referencing nested properties:
```java
final BeanPath<Person, String> personCityProperty = 
    $(Person::getContact).$(Contact::getAddress).$(Address::getCity);
assertEquals("contact.address.city", personCityProperty.getPath());
assertEquals(Person.class, personCityProperty.getBeanClass());
assertEquals(String.class, personCityProperty.getType());
```

#### Dynamic property resolution
```java
final BeanPath<Person, String> path = $(Person::getContact).$(Contact::getAddress).$(Address::getCity);
assertEquals(path, $(Person.class).$("contact").$("address").$("city"));
assertEquals(path, $(Person::getContact).$("address.city"));
assertEquals(path, $(Person.class).$("contact.address.city"));
assertEquals(path, $(Person.class,"contact.address.city"));
```
It is still possible to define paths using strings.

#### Get a set of paths to reference all properties of a bean (root or nested)   
```java
final Set<BeanPath<Person, ?>> personPropList = $(Person.class).all();
final Set<BeanPath<Person, ?>> addrPropList = $(Person::getContact).$(Contact::getAddress).all();
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
any). If it is not possible to instantiate a transitive bean an IncompletePathException will be thrown.
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

#### Collections support (experimental, may be changed or removed in future releases)

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
