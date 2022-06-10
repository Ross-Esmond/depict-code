function AppointmentField() {
  const [appointment, setAppointment] = useState()
  const asDate = dateInputToDate(appointment)

  return <div>
    <DateField value={appointment} onChange={setAppointment} />
    <Error show={asDate.valueOf() <= Date.now()}>
      Date must be in the future.
    </Error>
    <button onClick={handleSubmit}>Set Appointment</button>
  </div>
}
