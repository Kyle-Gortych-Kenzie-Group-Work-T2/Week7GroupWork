package com.kenzie.rackmonitor;

import com.google.common.base.Verify;
import com.kenzie.rackmonitor.*;
import com.kenzie.rackmonitor.clients.warranty.Warranty;
import com.kenzie.rackmonitor.clients.warranty.WarrantyClient;
import com.kenzie.rackmonitor.clients.warranty.WarrantyNotFoundException;
import com.kenzie.rackmonitor.clients.wingnut.WingnutClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
public class RackMonitorIncidentTest {
    RackMonitor rackMonitor;
    @Mock
    WingnutClient wingnutClient;
    @Mock
    WarrantyClient warrantyClient;
    @Mock
    Rack rack1;

    Server unhealthyServer = new Server("TEST0001");
    Server shakyServer = new Server("TEST0067");
    Map<Server, Integer> rack1ServerUnits;

    @BeforeEach
    void setUp() {
        initMocks(this);
//        warrantyClient = new WarrantyClient();
//        wingnutClient = new WingnutClient();
        rack1ServerUnits = new HashMap<>();
        rack1ServerUnits.put(unhealthyServer, 1);
//        rack1 = new Rack("RACK01", rack1ServerUnits);
       rackMonitor = new RackMonitor(new HashSet<>(Arrays.asList(rack1)),
            wingnutClient, warrantyClient, 0.9D, 0.8D);
    }

    @Test
    public void getIncidents_withOneUnhealthyServer_createsOneReplaceIncident() throws Exception {
        // GIVEN
        // The rack is set up with a single unhealthy server
        // We've reported the unhealthy server to Wingnut
       // rackMonitor.monitorRacks();
Map<Server,Double> serverResult= new HashMap<>();
serverResult.put(unhealthyServer,0.75D);
when(rack1.getHealth()).thenReturn(serverResult);
when(rack1.getUnitForServer(unhealthyServer)).thenReturn(1);
when(warrantyClient.getWarrantyForServer(unhealthyServer)).thenReturn(Warranty.nullWarranty());
        // WHEN
      //  Set<HealthIncident> actualIncidents = rackMonitor.getIncidents();
rackMonitor.monitorRacks();
Set<HealthIncident> actualIncidents=rackMonitor.getIncidents();
        // THEN

        HealthIncident expected =
                new HealthIncident(unhealthyServer, rack1, 1, RequestAction.REPLACE);
        assertTrue(actualIncidents.contains(expected),
                "Monitoring an unhealthy server should record a REPLACE incident!");
    }

    @Test
    public void getIncidents_withOneShakyServer_createsOneInspectIncident() throws Exception {
        // GIVEN
        Map<Server,Double> serverResult= new HashMap<>();
        serverResult.put(shakyServer,0.80D);
        // The rack is set up with a single shaky server
//        rack1ServerUnits = new HashMap<>();
//        rack1ServerUnits.put(shakyServer, 1);
//        rack1 = new Rack("RACK01", rack1ServerUnits);
//        rackMonitor = new RackMonitor(new HashSet<>(Arrays.asList(rack1)),
//            wingnutClient, warrantyClient, 0.9D, 0.8D);
//        // We've reported the shaky server to Wingnut
//        rackMonitor.monitorRacks();
when(rack1.getHealth()).thenReturn(serverResult);
when(rack1.getUnitForServer(shakyServer)).thenReturn(1);
when(warrantyClient.getWarrantyForServer(shakyServer)).thenReturn(Warranty.nullWarranty());
        // WHEN
        rackMonitor.monitorRacks();
        Set<HealthIncident> actualIncidents = rackMonitor.getIncidents();

        // THEN
        HealthIncident expected =
            new HealthIncident(shakyServer, rack1, 1, RequestAction.INSPECT);
        assertTrue(actualIncidents.contains(expected),
            "Monitoring a shaky server should record an INSPECT incident!");
    }

    @Test
    public void getIncidents_withOneHealthyServer_createsNoIncidents() throws Exception {
        // GIVEN
        // monitorRacks() will find only healthy servers
        when(rack1.getHealth()).thenReturn(Collections.singletonMap(new Server("HealthyServer"), 0.95D));
        // WHEN
        rackMonitor.monitorRacks();
        Set<HealthIncident> actualIncidents = rackMonitor.getIncidents();

        // THEN
        assertEquals(0, actualIncidents.size(),
            "Monitoring a healthy server should record no incidents!");
    }

    @Test
    public void monitorRacks_withOneUnhealthyServer_replacesServer() throws Exception {
        // GIVEN
        // The rack is set up with a single unhealthy server
        when(rack1.getHealth()).thenReturn(Collections.singletonMap(unhealthyServer, 0.7D));
        when(rack1.getUnitForServer(unhealthyServer)).thenReturn(1);
        when(warrantyClient.getWarrantyForServer(unhealthyServer)).thenReturn(Warranty.nullWarranty());
        // WHEN
        rackMonitor.monitorRacks();

        // THEN
        verify(warrantyClient).getWarrantyForServer(unhealthyServer);
        verify(wingnutClient).requestReplacement(rack1,1,Warranty.nullWarranty());
        // There were no exceptions
        // No way to tell we called the warrantyClient for the server's Warranty
        // No way to tell we called Wingnut to replace the server
    }

    @Test
    public void monitorRacks_withUnwarrantiedServer_throwsServerException() throws Exception {
        // GIVEN
        Server noWarrantyServer = new Server("TEST0052");
        rack1ServerUnits = new HashMap<>();
        rack1ServerUnits.put(noWarrantyServer, 1);
//        rack1 = new Rack("RACK01", rack1ServerUnits);
//        rackMonitor = new RackMonitor(new HashSet<>(Arrays.asList(rack1)),
//            wingnutClient, warrantyClient, 0.9D, 0.8D);
      //  Warranty mockWarranty = new Warranty("Warranty Details");
        when(warrantyClient.getWarrantyForServer(noWarrantyServer)).thenThrow(WarrantyNotFoundException.class);
        when(rack1.getUnitForServer(noWarrantyServer)).thenReturn(1);
        when(rack1.getHealth()).thenReturn(Collections.singletonMap(noWarrantyServer,0.7D));
        // WHEN and THEN
       // rackMonitor.monitorRacks();
        assertThrows(RackMonitorException.class,
            () -> rackMonitor.monitorRacks(),
            "Monitoring a server with no warranty should throw exception!");
    }
}
